/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.commands;

import com.alliander.osgp.dto.valueobjects.smartmetering.MeterReadsGas;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.openmuc.jdlms.AccessResultCode;
import org.openmuc.jdlms.ClientConnection;
import org.openmuc.jdlms.DataObject;
import org.openmuc.jdlms.GetRequestParameter;
import org.openmuc.jdlms.GetResult;
import org.openmuc.jdlms.ObisCode;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alliander.osgp.dto.valueobjects.smartmetering.PeriodType;
import com.alliander.osgp.dto.valueobjects.smartmetering.PeriodicMeterReadsContainerGas;
import com.alliander.osgp.dto.valueobjects.smartmetering.PeriodicMeterReadsQuery;

@Component()
public class GetPeriodicMeterReadsGasCommandExecutor implements
        CommandExecutor<PeriodicMeterReadsQuery, PeriodicMeterReadsContainerGas> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetPeriodicMeterReadsGasCommandExecutor.class);

    private static final int CLASS_ID_PROFILE_GENERIC = 7;
    private static final ObisCode OBIS_CODE_INTERVAL_MBUS_1 = new ObisCode("0.1.24.3.0.255");
    private static final ObisCode OBIS_CODE_INTERVAL_MBUS_2 = new ObisCode("0.2.24.3.0.255");
    private static final ObisCode OBIS_CODE_INTERVAL_MBUS_3 = new ObisCode("0.3.24.3.0.255");
    private static final ObisCode OBIS_CODE_INTERVAL_MBUS_4 = new ObisCode("0.4.24.3.0.255");
    private static final ObisCode OBIS_CODE_DAILY_BILLING = new ObisCode("1.0.99.2.0.255");
    private static final ObisCode OBIS_CODE_MONTHLY_BILLING = new ObisCode("0.0.98.1.0.255");
    private static final byte ATTRIBUTE_ID_BUFFER = 2;

    private static final int BUFFER_INDEX_CLOCK = 0;
    private static final int BUFFER_INDEX_AMR_STATUS = 1;
    private static final int BUFFER_INDEX_MBUS_VALUE_FIRST = 6;
    private static final int BUFFER_INDEX_MBUS_CAPTURETIME_FIRST = 7;
    private static final int BUFFER_INDEX_MBUS_VALUE_INT = 2;
    private static final int BUFFER_INDEX_MBUS_CAPTURETIME_INT = 3;

    @Autowired
    private DlmsHelperService dlmsHelperService;

    @Override
    public PeriodicMeterReadsContainerGas execute(final ClientConnection conn,
            final PeriodicMeterReadsQuery periodicMeterReadsRequest) throws IOException, ProtocolAdapterException {

        final PeriodType periodType;
        final DateTime beginDateTime;
        final DateTime endDateTime;
        if (periodicMeterReadsRequest != null) {
            periodType = periodicMeterReadsRequest.getPeriodType();
            beginDateTime = new DateTime(periodicMeterReadsRequest.getBeginDate());
            endDateTime = new DateTime(periodicMeterReadsRequest.getEndDate());
        } else {
            throw new IllegalArgumentException(
                    "PeriodicMeterReadsRequestData should contain PeriodType, BeginDate and EndDate.");
        }

        final GetRequestParameter profileBuffer = this.getProfileBuffer(periodType,
                periodicMeterReadsRequest.getChannel());
        // TODO: implement selective access

        LOGGER.debug(
                "Retrieving current billing period and profiles for class id: {}, obis code: {}, attribute id: {}",
                profileBuffer.classId(), profileBuffer.obisCode(), profileBuffer.attributeId());

        final List<GetResult> getResultList = conn.get(profileBuffer);

        checkResultList(getResultList);

        final List<MeterReadsGas> periodicMeterReads = new ArrayList<>();

        final GetResult getResult = getResultList.get(0);
        final AccessResultCode resultCode = getResult.resultCode();
        LOGGER.debug("AccessResultCode: {}({})", resultCode.name(), resultCode.value());
        final DataObject resultData = getResult.resultData();
        LOGGER.debug(this.dlmsHelperService.getDebugInfo(resultData));
        final List<DataObject> bufferedObjectsList = resultData.value();

        for (final DataObject bufferedObject : bufferedObjectsList) {
            final List<DataObject> bufferedObjects = bufferedObject.value();
            this.processNextPeriodicMeterReads(periodType, beginDateTime, endDateTime, periodicMeterReads,
                    bufferedObjects, periodicMeterReadsRequest.getChannel());
        }

        return new PeriodicMeterReadsContainerGas(periodType, periodicMeterReads);
    }

    private void processNextPeriodicMeterReads(final PeriodType periodType, final DateTime beginDateTime,
            final DateTime endDateTime, final List<MeterReadsGas> periodicMeterReads,
            final List<DataObject> bufferedObjects, final int channel) {

        final DataObject clock = bufferedObjects.get(BUFFER_INDEX_CLOCK);
        final DateTime bufferedDateTime = this.dlmsHelperService.fromDateTimeValue((byte[]) clock.value());
        if (bufferedDateTime.isBefore(beginDateTime) || bufferedDateTime.isAfter(endDateTime)) {
            final DateTimeFormatter dtf = ISODateTimeFormat.dateTime();
            LOGGER.warn("Not using an object from capture buffer (clock=" + dtf.print(bufferedDateTime)
                    + "), because the date does not match the given period: [" + dtf.print(beginDateTime) + " .. "
                    + dtf.print(endDateTime) + "].");
            return;
        }

        LOGGER.debug("Processing profile (" + periodType + ") objects captured at: {}",
                this.dlmsHelperService.getDebugInfo(clock));

        switch (periodType) {
        case INTERVAL:
            this.processNextPeriodicMeterReadsForInterval(periodicMeterReads, bufferedObjects, bufferedDateTime);
            break;
        case DAILY:
            this.processNextPeriodicMeterReadsForDaily(periodicMeterReads, bufferedObjects, bufferedDateTime, channel);
            break;
        case MONTHLY:
            this.processNextPeriodicMeterReadsForMonthly(periodicMeterReads, bufferedObjects, bufferedDateTime, channel);
            break;
        default:
            throw new AssertionError("Unknown PeriodType: " + periodType);
        }
    }

    private void processNextPeriodicMeterReadsForInterval(final List<MeterReadsGas> periodicMeterReads,
            final List<DataObject> bufferedObjects, final DateTime bufferedDateTime) {

        final DataObject amrStatus = bufferedObjects.get(BUFFER_INDEX_AMR_STATUS);
        LOGGER.warn("TODO - handle amrStatus ({})", this.dlmsHelperService.getDebugInfo(amrStatus));

        final DataObject gasValue = bufferedObjects.get(BUFFER_INDEX_MBUS_VALUE_INT);
        LOGGER.debug("gasValue: {}", this.dlmsHelperService.getDebugInfo(gasValue));

        final DataObject gasCaptureTime = bufferedObjects.get(BUFFER_INDEX_MBUS_CAPTURETIME_INT);
        LOGGER.debug("gasCaptureTime: {}", this.dlmsHelperService.getDebugInfo(gasCaptureTime));

        final MeterReadsGas nextPeriodicMeterReads = new MeterReadsGas(bufferedDateTime.toDate(),
                (Long) gasValue.value(), this.dlmsHelperService.fromDateTimeValue((byte[]) gasCaptureTime.value())
                        .toDate());
        periodicMeterReads.add(nextPeriodicMeterReads);
    }

    private void processNextPeriodicMeterReadsForDaily(final List<MeterReadsGas> periodicMeterReads,
            final List<DataObject> bufferedObjects, final DateTime bufferedDateTime, final int channel) {

        final DataObject amrStatus = bufferedObjects.get(BUFFER_INDEX_AMR_STATUS);
        LOGGER.warn("TODO - handle amrStatus ({})", this.dlmsHelperService.getDebugInfo(amrStatus));

        // calculate offset from first entry
        int offset = (channel - 1) * 2;

        final DataObject gasValue = bufferedObjects.get(BUFFER_INDEX_MBUS_VALUE_FIRST + offset);
        LOGGER.debug("gasValue: {}", this.dlmsHelperService.getDebugInfo(gasValue));
        final DataObject gasCaptureTime = bufferedObjects.get(BUFFER_INDEX_MBUS_CAPTURETIME_FIRST + offset);
        LOGGER.debug("gasCaptureTime: {}", this.dlmsHelperService.getDebugInfo(gasCaptureTime));

        final MeterReadsGas nextPeriodicMeterReads = new MeterReadsGas(bufferedDateTime.toDate(),
                (Long) gasValue.value(), this.dlmsHelperService.fromDateTimeValue((byte[]) gasCaptureTime.value())
                        .toDate());
        periodicMeterReads.add(nextPeriodicMeterReads);
    }

    private void processNextPeriodicMeterReadsForMonthly(final List<MeterReadsGas> periodicMeterReads,
            final List<DataObject> bufferedObjects, final DateTime bufferedDateTime, final int channel) {

        /*
         * TODO: look into AMR Profile status for MONTHLY.
         */

        // calculate offset from first entry, -1 because MONTHLY has no AMR
        // entry
        int offset = ((channel - 1) * 2) - 1;

        final DataObject gasValue = bufferedObjects.get(BUFFER_INDEX_MBUS_VALUE_FIRST + offset);
        LOGGER.debug("gasValue: {}", this.dlmsHelperService.getDebugInfo(gasValue));
        final DataObject gasCaptureTime = bufferedObjects.get(BUFFER_INDEX_MBUS_CAPTURETIME_FIRST + offset);
        LOGGER.debug("gasCaptureTime: {}", this.dlmsHelperService.getDebugInfo(gasCaptureTime));

        final MeterReadsGas nextPeriodicMeterReads = new MeterReadsGas(bufferedDateTime.toDate(),
                (Long) gasValue.value(), this.dlmsHelperService.fromDateTimeValue((byte[]) gasCaptureTime.value())
                        .toDate());
        periodicMeterReads.add(nextPeriodicMeterReads);
    }

    private static void checkResultList(final List<GetResult> getResultList) throws ProtocolAdapterException {
        if (getResultList.isEmpty()) {
            throw new ProtocolAdapterException(
                    "No GetResult received while retrieving current billing period and profiles.");
        }

        if (getResultList.size() > 1) {
            LOGGER.info("Expected 1 GetResult while retrieving current billing period and profiles, got "
                    + getResultList.size());
        }
    }

    private ObisCode intervalForChannel(final int channel) throws ProtocolAdapterException {
        switch (channel) {
        case 1:
            return OBIS_CODE_INTERVAL_MBUS_1;
        case 2:
            return OBIS_CODE_INTERVAL_MBUS_2;
        case 3:
            return OBIS_CODE_INTERVAL_MBUS_3;
        case 4:
            return OBIS_CODE_INTERVAL_MBUS_4;
        default:
            throw new ProtocolAdapterException(String.format("channel %s not supported", channel));
        }
    }

    private GetRequestParameter getProfileBuffer(final PeriodType periodType, final int channel)
            throws ProtocolAdapterException {
        GetRequestParameter profileBuffer;

        switch (periodType) {
        case INTERVAL:
            profileBuffer = new GetRequestParameter(CLASS_ID_PROFILE_GENERIC, this.intervalForChannel(channel),
                    ATTRIBUTE_ID_BUFFER);
            break;
        case DAILY:
            profileBuffer = new GetRequestParameter(CLASS_ID_PROFILE_GENERIC, OBIS_CODE_DAILY_BILLING,
                    ATTRIBUTE_ID_BUFFER);
            break;
        case MONTHLY:
            profileBuffer = new GetRequestParameter(CLASS_ID_PROFILE_GENERIC, OBIS_CODE_MONTHLY_BILLING,
                    ATTRIBUTE_ID_BUFFER);
            break;
        default:
            throw new ProtocolAdapterException(String.format("periodtype %s not supported", periodType));
        }
        return profileBuffer;
    }

}
