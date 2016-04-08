/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.openmuc.jdlms.AttributeAddress;
import org.openmuc.jdlms.ClientConnection;
import org.openmuc.jdlms.GetResult;
import org.openmuc.jdlms.ObisCode;
import org.openmuc.jdlms.SelectiveAccessDescription;
import org.openmuc.jdlms.datatypes.DataObject;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alliander.osgp.dto.valueobjects.smartmetering.AmrProfileStatusCode;
import com.alliander.osgp.dto.valueobjects.smartmetering.AmrProfileStatusCodeFlag;
import com.alliander.osgp.dto.valueobjects.smartmetering.Channel;
import com.alliander.osgp.dto.valueobjects.smartmetering.CosemDateTime;
import com.alliander.osgp.dto.valueobjects.smartmetering.PeriodType;
import com.alliander.osgp.dto.valueobjects.smartmetering.PeriodicMeterReadsContainerGas;
import com.alliander.osgp.dto.valueobjects.smartmetering.PeriodicMeterReadsGas;
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
    private static final byte ATTRIBUTE_ID_SCALER_UNIT = 3;
    private static final ObisCode OBIS_CODE_MBUS_1_SCALER_UNIT = new ObisCode("0.1.24.2.1.255");
    private static final ObisCode OBIS_CODE_MBUS_2_SCALER_UNIT = new ObisCode("0.2.24.2.1.255");
    private static final ObisCode OBIS_CODE_MBUS_3_SCALER_UNIT = new ObisCode("0.3.24.2.1.255");
    private static final ObisCode OBIS_CODE_MBUS_4_SCALER_UNIT = new ObisCode("0.4.24.2.1.255");
    private static final int RESULT_INDEX_SCALER_UNIT = 1;
    private static final int CLASS_ID_EXTENDED_REGISTER = 4;

    private static final int ACCESS_SELECTOR_RANGE_DESCRIPTOR = 1;

    private static final int BUFFER_INDEX_CLOCK = 0;
    private static final int BUFFER_INDEX_AMR_STATUS = 1;
    private static final int BUFFER_INDEX_MBUS_VALUE_INT = 2;
    private static final int BUFFER_INDEX_MBUS_CAPTURETIME_INT = 3;

    private static final int CLASS_ID_MBUS = 4;
    private static final Map<Integer, byte[]> OBIS_BYTES_M_BUS_MASTER_VALUE_1_CHANNEL_MAP = new HashMap<Integer, byte[]>();
    static {
        OBIS_BYTES_M_BUS_MASTER_VALUE_1_CHANNEL_MAP.put(1, new byte[] { 0, 1, 24, 2, 1, (byte) 255 });
        OBIS_BYTES_M_BUS_MASTER_VALUE_1_CHANNEL_MAP.put(2, new byte[] { 0, 2, 24, 2, 1, (byte) 255 });
        OBIS_BYTES_M_BUS_MASTER_VALUE_1_CHANNEL_MAP.put(3, new byte[] { 0, 3, 24, 2, 1, (byte) 255 });
        OBIS_BYTES_M_BUS_MASTER_VALUE_1_CHANNEL_MAP.put(4, new byte[] { 0, 4, 24, 2, 1, (byte) 255 });
    }
    private static final byte ATTRIBUTE_M_BUS_MASTER_VALUE = 2;
    private static final byte ATTRIBUTE_M_BUS_MASTER_VALUE_CAPTURE_TIME = 5;

    private static final Map<Integer, Integer> INDEX_MONTHLY_SELECTIVE_ACCESS_MBUS_VALUE_MAP = new HashMap<Integer, Integer>();
    private static final Map<Integer, Integer> INDEX_MONTHLY_SELECTIVE_ACCESS_MBUS_VALUE_CAPTURE_TIME_MAP = new HashMap<Integer, Integer>();
    private static final Map<Integer, Integer> INDEX_MONTHLY_MBUS_VALUE_MAP = new HashMap<Integer, Integer>();
    private static final Map<Integer, Integer> INDEX_MONTHLY_MBUS_VALUE_CAPTURE_TIME_MAP = new HashMap<Integer, Integer>();
    static {
        // Indicates the position of the value or value_capture_time for
        // channel 1, 2, 3 and 4

        /*-
         * When specific capture_objects are selected with selective access:
         *
         * {8,0-0:1.0.0.255,2,0};                                       position 0
         *
         * Value and capture time for one of the four channels will be selected,
         * for which position 1 and 2 are used.
         *
         * {4,0-1:24.2.1.255,2,0};   value channel 1                    position 1
         * {4,0-1:24.2.1.255,5,0};   value capture time channel 1       position 2
         * {4,0-2:24.2.1.255,2,0};   value channel 2                    position 1
         * {4,0-2:24.2.1.255,5,0};   value capture time channel 2       position 2
         * {4,0-3:24.2.1.255,2,0};   value channel 3                    position 1
         * {4,0-3:24.2.1.255,5,0};   value capture time channel 3       position 2
         * {4,0-4:24.2.1.255,2,0};   value channel 4                    position 1
         * {4,0-4:24.2.1.255,5,0};   value capture time channel 4       position 2
         */
        INDEX_MONTHLY_SELECTIVE_ACCESS_MBUS_VALUE_MAP.put(1, 1);
        INDEX_MONTHLY_SELECTIVE_ACCESS_MBUS_VALUE_CAPTURE_TIME_MAP.put(1, 2);
        INDEX_MONTHLY_SELECTIVE_ACCESS_MBUS_VALUE_MAP.put(2, 1);
        INDEX_MONTHLY_SELECTIVE_ACCESS_MBUS_VALUE_CAPTURE_TIME_MAP.put(2, 2);
        INDEX_MONTHLY_SELECTIVE_ACCESS_MBUS_VALUE_MAP.put(3, 1);
        INDEX_MONTHLY_SELECTIVE_ACCESS_MBUS_VALUE_CAPTURE_TIME_MAP.put(3, 2);
        INDEX_MONTHLY_SELECTIVE_ACCESS_MBUS_VALUE_MAP.put(4, 1);
        INDEX_MONTHLY_SELECTIVE_ACCESS_MBUS_VALUE_CAPTURE_TIME_MAP.put(4, 2);

        /*-
         * When no specific capture_objects are selected with selective access:
         *
         * {8,0-0:1.0.0.255,2,0};                                    position 0
         * {3,1-0:1.8.1.255,2,0};                                    position 1
         * {3,1-0:1.8.2.255,2,0};                                    position 2
         * {3,1-0:2.8.1.255,2,0};                                    position 3
         * {3,1-0:2.8.2.255,2,0};                                    position 4
         * {4,0-1:24.2.1.255,2,0};   value channel 1                 position 5
         * {4,0-1:24.2.1.255,5,0};   value capture time channel 1    position 6
         * {4,0-2:24.2.1.255,2,0};   value channel 2                 position 7
         * {4,0-2:24.2.1.255,5,0};   value capture time channel 2    position 8
         * {4,0-3:24.2.1.255,2,0};   value channel 3                 position 9
         * {4,0-3:24.2.1.255,5,0};   value capture time channel 3    position 10
         * {4,0-4:24.2.1.255,2,0};   value channel 4                 position 11
         * {4,0-4:24.2.1.255,5,0};   value capture time channel 4    position 12
         */
        INDEX_MONTHLY_MBUS_VALUE_MAP.put(1, 5);
        INDEX_MONTHLY_MBUS_VALUE_CAPTURE_TIME_MAP.put(1, 6);
        INDEX_MONTHLY_MBUS_VALUE_MAP.put(2, 7);
        INDEX_MONTHLY_MBUS_VALUE_CAPTURE_TIME_MAP.put(2, 8);
        INDEX_MONTHLY_MBUS_VALUE_MAP.put(3, 9);
        INDEX_MONTHLY_MBUS_VALUE_CAPTURE_TIME_MAP.put(3, 10);
        INDEX_MONTHLY_MBUS_VALUE_MAP.put(4, 11);
        INDEX_MONTHLY_MBUS_VALUE_CAPTURE_TIME_MAP.put(4, 12);
    }

    private static final Map<Integer, Integer> INDEX_DAILY_MBUS_VALUE_MAP = new HashMap<Integer, Integer>();
    private static final Map<Integer, Integer> INDEX_DAILY_MBUS_VALUE_CAPTURE_TIME_MAP = new HashMap<Integer, Integer>();
    private static final Map<Integer, Integer> INDEX_DAILY_SELECTIVE_ACCESS_MBUS_VALUE_MAP = new HashMap<Integer, Integer>();
    private static final Map<Integer, Integer> INDEX_DAILY_SELECTIVE_ACCESS_MBUS_VALUE_CAPTURE_TIME_MAP = new HashMap<Integer, Integer>();
    static {
        // Indicates the position of the value or value_capture_time for
        // channel 1, 2, 3 and 4

        /*-
         * When no specific capture_objects are selected with selective access:
         *
         * {8,0-0:1.0.0.255,2,0};                                    position 0
         * {1,0-0:96.10.2.255,2,0}                                   position 1
         * {3,1-0:1.8.1.255,2,0};                                    position 2
         * {3,1-0:1.8.2.255,2,0};                                    position 3
         * {3,1-0:2.8.1.255,2,0};                                    position 4
         * {3,1-0:2.8.2.255,2,0};                                    position 5
         * {4,0-1:24.2.1.255,2,0};   value channel 1                 position 6
         * {4,0-1:24.2.1.255,5,0};   value capture time channel 1    position 7
         * {4,0-2:24.2.1.255,2,0};   value channel 2                 position 8
         * {4,0-2:24.2.1.255,5,0};   value capture time channel 2    position 9
         * {4,0-3:24.2.1.255,2,0};   value channel 3                 position 10
         * {4,0-3:24.2.1.255,5,0};   value capture time channel 3    position 11
         * {4,0-4:24.2.1.255,2,0};   value channel 4                 position 12
         * {4,0-4:24.2.1.255,5,0};   value capture time channel 4    position 13
         */
        INDEX_DAILY_MBUS_VALUE_MAP.put(1, 6);
        INDEX_DAILY_MBUS_VALUE_CAPTURE_TIME_MAP.put(1, 7);
        INDEX_DAILY_MBUS_VALUE_MAP.put(2, 8);
        INDEX_DAILY_MBUS_VALUE_CAPTURE_TIME_MAP.put(2, 9);
        INDEX_DAILY_MBUS_VALUE_MAP.put(3, 10);
        INDEX_DAILY_MBUS_VALUE_CAPTURE_TIME_MAP.put(3, 11);
        INDEX_DAILY_MBUS_VALUE_MAP.put(4, 12);
        INDEX_DAILY_MBUS_VALUE_CAPTURE_TIME_MAP.put(4, 13);

        /*-
         * When specific capture_objects are selected with selective access:
         *
         * {8,0-0:1.0.0.255,2,0};                                    position 0
         * {1,0-0:96.10.2.255,2,0}                                   position 1
         *
         * Value and capture time for one of the four channels will be selected,
         * for which position 2 and 3 are used.
         *
         * {4,0-1:24.2.1.255,2,0};   value channel 1                 position 2
         * {4,0-1:24.2.1.255,5,0};   value capture time channel 1    position 3
         * {4,0-2:24.2.1.255,2,0};   value channel 2                 position 2
         * {4,0-2:24.2.1.255,5,0};   value capture time channel 2    position 3
         * {4,0-3:24.2.1.255,2,0};   value channel 3                 position 2
         * {4,0-3:24.2.1.255,5,0};   value capture time channel 3    position 3
         * {4,0-4:24.2.1.255,2,0};   value channel 4                 position 2
         * {4,0-4:24.2.1.255,5,0};   value capture time channel 4    position 3
         */
        INDEX_DAILY_SELECTIVE_ACCESS_MBUS_VALUE_MAP.put(1, 2);
        INDEX_DAILY_SELECTIVE_ACCESS_MBUS_VALUE_CAPTURE_TIME_MAP.put(1, 3);
        INDEX_DAILY_SELECTIVE_ACCESS_MBUS_VALUE_MAP.put(2, 2);
        INDEX_DAILY_SELECTIVE_ACCESS_MBUS_VALUE_CAPTURE_TIME_MAP.put(2, 3);
        INDEX_DAILY_SELECTIVE_ACCESS_MBUS_VALUE_MAP.put(3, 2);
        INDEX_DAILY_SELECTIVE_ACCESS_MBUS_VALUE_CAPTURE_TIME_MAP.put(3, 3);
        INDEX_DAILY_SELECTIVE_ACCESS_MBUS_VALUE_MAP.put(4, 2);
        INDEX_DAILY_SELECTIVE_ACCESS_MBUS_VALUE_CAPTURE_TIME_MAP.put(4, 3);
    }

    @Autowired
    private DlmsHelperService dlmsHelperService;

    @Autowired
    private AmrProfileStatusCodeHelperService amrProfileStatusCodeHelperService;

    @Override
    public PeriodicMeterReadsContainerGas execute(final ClientConnection conn, final DlmsDevice device,
            final PeriodicMeterReadsQuery periodicMeterReadsQuery) throws ProtocolAdapterException {

        final PeriodType periodType;
        final DateTime beginDateTime;
        final DateTime endDateTime;
        if (periodicMeterReadsQuery != null) {
            periodType = periodicMeterReadsQuery.getPeriodType();
            beginDateTime = new DateTime(periodicMeterReadsQuery.getBeginDate());
            endDateTime = new DateTime(periodicMeterReadsQuery.getEndDate());
        } else {
            throw new IllegalArgumentException(
                    "PeriodicMeterReadsQuery should contain PeriodType, BeginDate and EndDate.");
        }

        final AttributeAddress[] profileBufferAndScalerUnit = this.getProfileBufferAndScalerUnit(periodType,
                periodicMeterReadsQuery.getChannel(), beginDateTime, endDateTime, device.isSelectiveAccessSupported());

        LOGGER.debug("Retrieving current billing period and profiles for gas for period type: {}, from: {}, to: {}",
                periodType, beginDateTime, endDateTime);

        /*
         * workaround for a problem when using with_list and retrieving a
         * profile buffer, this will be returned erroneously.
         */
        final List<GetResult> getResultList = new ArrayList<GetResult>(profileBufferAndScalerUnit.length);
        for (final AttributeAddress address : profileBufferAndScalerUnit) {
            getResultList.addAll(this.dlmsHelperService.getAndCheck(conn, device, "retrieve periodic meter reads for "
                    + periodType + ", channel " + periodicMeterReadsQuery.getChannel(), address));
        }

        final List<PeriodicMeterReadsGas> periodicMeterReads = new ArrayList<>();

        final DataObject resultData = this.dlmsHelperService.readDataObject(getResultList.get(0),
                "Periodic G-Meter Reads");
        final List<DataObject> bufferedObjectsList = resultData.value();

        for (final DataObject bufferedObject : bufferedObjectsList) {
            final List<DataObject> bufferedObjects = bufferedObject.value();
            this.processNextPeriodicMeterReads(periodType, beginDateTime, endDateTime, periodicMeterReads,
                    bufferedObjects, periodicMeterReadsQuery.getChannel(), device.isSelectiveAccessSupported(),
                    getResultList);
        }

        return new PeriodicMeterReadsContainerGas(periodType, periodicMeterReads);
    }

    private void processNextPeriodicMeterReads(final PeriodType periodType, final DateTime beginDateTime,
            final DateTime endDateTime, final List<PeriodicMeterReadsGas> periodicMeterReads,
            final List<DataObject> bufferedObjects, final Channel channel, final boolean isSelectiveAccessSupported,
            final List<GetResult> results) throws ProtocolAdapterException {

        final CosemDateTime cosemDateTime = this.dlmsHelperService.readDateTime(
                bufferedObjects.get(BUFFER_INDEX_CLOCK), "Clock from " + periodType + " buffer gas");
        final DateTime bufferedDateTime = cosemDateTime == null ? null : cosemDateTime.asDateTime();
        if (bufferedDateTime == null) {
            final DateTimeFormatter dtf = ISODateTimeFormat.dateTime();
            LOGGER.warn("Not using an object from capture buffer (clock=" + cosemDateTime
                    + "), because the date does not match the given period, since it is not fully specified: ["
                    + dtf.print(beginDateTime) + " .. " + dtf.print(endDateTime) + "].");
            return;
        }
        if (bufferedDateTime.isBefore(beginDateTime) || bufferedDateTime.isAfter(endDateTime)) {
            final DateTimeFormatter dtf = ISODateTimeFormat.dateTime();
            LOGGER.warn("Not using an object from capture buffer (clock=" + dtf.print(bufferedDateTime)
                    + "), because the date does not match the given period: [" + dtf.print(beginDateTime) + " .. "
                    + dtf.print(endDateTime) + "].");
            return;
        }

        LOGGER.debug("Processing profile (" + periodType + ") objects captured at: {}", cosemDateTime);

        switch (periodType) {
        case INTERVAL:
            this.processNextPeriodicMeterReadsForInterval(periodicMeterReads, bufferedObjects, bufferedDateTime,
                    results);
            break;
        case DAILY:
            this.processNextPeriodicMeterReadsForDaily(periodicMeterReads, bufferedObjects, bufferedDateTime, channel,
                    isSelectiveAccessSupported, results);
            break;
        case MONTHLY:
            this.processNextPeriodicMeterReadsForMonthly(periodicMeterReads, bufferedObjects, bufferedDateTime,
                    channel, isSelectiveAccessSupported, results);
            break;
        default:
            throw new AssertionError("Unknown PeriodType: " + periodType);
        }
    }

    private void processNextPeriodicMeterReadsForInterval(final List<PeriodicMeterReadsGas> periodicMeterReads,
            final List<DataObject> bufferedObjects, final DateTime bufferedDateTime, final List<GetResult> results)
            throws ProtocolAdapterException {

        final AmrProfileStatusCode amrProfileStatusCode = this.readAmrProfileStatusCode(bufferedObjects
                .get(BUFFER_INDEX_AMR_STATUS));

        final DataObject gasValue = bufferedObjects.get(BUFFER_INDEX_MBUS_VALUE_INT);
        LOGGER.debug("gasValue: {}", this.dlmsHelperService.getDebugInfo(gasValue));

        final CosemDateTime cosemDateTime = this.dlmsHelperService.readDateTime(
                bufferedObjects.get(BUFFER_INDEX_MBUS_CAPTURETIME_INT), "Clock from mbus interval extended register");
        final Date captureTime;
        if (cosemDateTime.isDateTimeSpecified()) {
            captureTime = cosemDateTime.asDateTime().toDate();
        } else {
            throw new ProtocolAdapterException("Unexpected null/unspecified value for Gas Capture Time");
        }
        final PeriodicMeterReadsGas nextPeriodicMeterReads = new PeriodicMeterReadsGas(bufferedDateTime.toDate(),
                this.dlmsHelperService.getScaledMeterValue(gasValue,
                        results.get(RESULT_INDEX_SCALER_UNIT).resultData(), "gasValue"), captureTime,
                        amrProfileStatusCode);
        periodicMeterReads.add(nextPeriodicMeterReads);
    }

    private void processNextPeriodicMeterReadsForDaily(final List<PeriodicMeterReadsGas> periodicMeterReads,
            final List<DataObject> bufferedObjects, final DateTime bufferedDateTime, final Channel channel,
            final boolean isSelectiveAccessSupported, final List<GetResult> results) throws ProtocolAdapterException {

        final AmrProfileStatusCode amrProfileStatusCode = this.readAmrProfileStatusCode(bufferedObjects
                .get(BUFFER_INDEX_AMR_STATUS));

        DataObject gasValue = null;
        DataObject gasCaptureTime = null;
        if (isSelectiveAccessSupported) {
            gasValue = bufferedObjects.get(INDEX_DAILY_SELECTIVE_ACCESS_MBUS_VALUE_MAP.get(channel.getChannelNumber()));
            gasCaptureTime = bufferedObjects.get(INDEX_DAILY_SELECTIVE_ACCESS_MBUS_VALUE_CAPTURE_TIME_MAP.get(channel
                    .getChannelNumber()));
        } else {
            gasValue = bufferedObjects.get(INDEX_DAILY_MBUS_VALUE_MAP.get(channel.getChannelNumber()));
            gasCaptureTime = bufferedObjects
                    .get(INDEX_DAILY_MBUS_VALUE_CAPTURE_TIME_MAP.get(channel.getChannelNumber()));
        }

        LOGGER.debug("gasValue: {}", this.dlmsHelperService.getDebugInfo(gasValue));
        LOGGER.debug("gasCaptureTime: {}", this.dlmsHelperService.getDebugInfo(gasCaptureTime));

        final CosemDateTime cosemDateTime = this.dlmsHelperService.readDateTime(gasCaptureTime,
                "Clock from daily mbus daily extended register");
        final Date captureTime;
        if (cosemDateTime.isDateTimeSpecified()) {
            captureTime = cosemDateTime.asDateTime().toDate();
        } else {
            throw new ProtocolAdapterException("Unexpected null/unspecified value for Gas Capture Time");
        }
        final PeriodicMeterReadsGas nextPeriodicMeterReads = new PeriodicMeterReadsGas(bufferedDateTime.toDate(),
                this.dlmsHelperService.getScaledMeterValue(gasValue,
                        results.get(RESULT_INDEX_SCALER_UNIT).resultData(), "gasValue"), captureTime,
                amrProfileStatusCode);
        periodicMeterReads.add(nextPeriodicMeterReads);
    }

    private void processNextPeriodicMeterReadsForMonthly(final List<PeriodicMeterReadsGas> periodicMeterReads,
            final List<DataObject> bufferedObjects, final DateTime bufferedDateTime, final Channel channel,
            final boolean isSelectiveAccessSupported, final List<GetResult> results) throws ProtocolAdapterException {

        DataObject gasValue = null;
        DataObject gasCaptureTime = null;
        if (isSelectiveAccessSupported) {
            gasValue = bufferedObjects
                    .get(INDEX_MONTHLY_SELECTIVE_ACCESS_MBUS_VALUE_MAP.get(channel.getChannelNumber()));
            gasCaptureTime = bufferedObjects.get(INDEX_MONTHLY_SELECTIVE_ACCESS_MBUS_VALUE_CAPTURE_TIME_MAP.get(channel
                    .getChannelNumber()));
        } else {
            gasValue = bufferedObjects.get(INDEX_MONTHLY_MBUS_VALUE_MAP.get(channel.getChannelNumber()));
            gasCaptureTime = bufferedObjects.get(INDEX_MONTHLY_MBUS_VALUE_CAPTURE_TIME_MAP.get(channel
                    .getChannelNumber()));
        }

        LOGGER.debug("gasValue: {}", this.dlmsHelperService.getDebugInfo(gasValue));
        LOGGER.debug("gasCaptureTime: {}", this.dlmsHelperService.getDebugInfo(gasCaptureTime));

        final CosemDateTime cosemDateTime = this.dlmsHelperService.readDateTime(gasCaptureTime,
                "gas capture time for mbus monthly");
        final Date captureTime;
        if (cosemDateTime.isDateTimeSpecified()) {
            captureTime = cosemDateTime.asDateTime().toDate();
        } else {
            throw new ProtocolAdapterException("Unexpected null/unspecified value for Gas Capture Time");
        }
        final PeriodicMeterReadsGas nextPeriodicMeterReads = new PeriodicMeterReadsGas(bufferedDateTime.toDate(),
                this.dlmsHelperService.getScaledMeterValue(gasValue,
                        results.get(RESULT_INDEX_SCALER_UNIT).resultData(), "gasValue"), captureTime);
        periodicMeterReads.add(nextPeriodicMeterReads);
    }

    private ObisCode intervalForChannel(final Channel channel) throws ProtocolAdapterException {
        switch (channel) {
        case ONE:
            return OBIS_CODE_INTERVAL_MBUS_1;
        case TWO:
            return OBIS_CODE_INTERVAL_MBUS_2;
        case THREE:
            return OBIS_CODE_INTERVAL_MBUS_3;
        case FOUR:
            return OBIS_CODE_INTERVAL_MBUS_4;
        default:
            throw new ProtocolAdapterException(String.format("channel %s not supported", channel));
        }
    }

    private AttributeAddress[] getProfileBufferAndScalerUnit(final PeriodType periodType, final Channel channel,
            final DateTime beginDateTime, final DateTime endDateTime, final boolean isSelectingValuesSupported)
            throws ProtocolAdapterException {

        final SelectiveAccessDescription access = this.getSelectiveAccessDescription(channel, periodType,
                beginDateTime, endDateTime, isSelectingValuesSupported);

        final List<AttributeAddress> profileBuffer = new ArrayList<AttributeAddress>();
        switch (periodType) {
        case INTERVAL:
            profileBuffer.add(new AttributeAddress(CLASS_ID_PROFILE_GENERIC, this.intervalForChannel(channel),
                    ATTRIBUTE_ID_BUFFER, access));
            break;
        case DAILY:
            profileBuffer.add(new AttributeAddress(CLASS_ID_PROFILE_GENERIC, OBIS_CODE_DAILY_BILLING,
                    ATTRIBUTE_ID_BUFFER, access));
            break;
        case MONTHLY:
            profileBuffer.add(new AttributeAddress(CLASS_ID_PROFILE_GENERIC, OBIS_CODE_MONTHLY_BILLING,
                    ATTRIBUTE_ID_BUFFER, access));
            break;
        default:
            throw new ProtocolAdapterException(String.format("periodtype %s not supported", periodType));
        }
        profileBuffer.add(this.getScalerUnit(channel));
        return profileBuffer.toArray(new AttributeAddress[profileBuffer.size()]);
    }

    private AttributeAddress getScalerUnit(final Channel channel) throws ProtocolAdapterException {

        switch (channel) {
        case ONE:
            return new AttributeAddress(CLASS_ID_EXTENDED_REGISTER, OBIS_CODE_MBUS_1_SCALER_UNIT,
                    ATTRIBUTE_ID_SCALER_UNIT);
        case TWO:
            return new AttributeAddress(CLASS_ID_EXTENDED_REGISTER, OBIS_CODE_MBUS_2_SCALER_UNIT,
                    ATTRIBUTE_ID_SCALER_UNIT);
        case THREE:
            return new AttributeAddress(CLASS_ID_EXTENDED_REGISTER, OBIS_CODE_MBUS_3_SCALER_UNIT,
                    ATTRIBUTE_ID_SCALER_UNIT);
        case FOUR:
            return new AttributeAddress(CLASS_ID_EXTENDED_REGISTER, OBIS_CODE_MBUS_4_SCALER_UNIT,
                    ATTRIBUTE_ID_SCALER_UNIT);
        default:
            throw new ProtocolAdapterException(String.format("channel %s not supported", channel));
        }
    }

    private SelectiveAccessDescription getSelectiveAccessDescription(final Channel channel,
            final PeriodType periodType, final DateTime beginDateTime, final DateTime endDateTime,
            final boolean isSelectingValuesSupported) {

        final int accessSelector = ACCESS_SELECTOR_RANGE_DESCRIPTOR;

        /*
         * Define the clock object {8,0-0:1.0.0.255,2,0} to be used as
         * restricting object in a range descriptor with a from value and to
         * value to determine which elements from the buffered array should be
         * retrieved.
         */
        final DataObject clockDefinition = this.dlmsHelperService.getClockDefinition();
        final DataObject fromValue = this.dlmsHelperService.asDataObject(beginDateTime);
        final DataObject toValue = this.dlmsHelperService.asDataObject(endDateTime);

        final List<DataObject> objectDefinitions = new ArrayList<>();
        if (isSelectingValuesSupported) {
            this.addSelectedValues(channel, periodType, objectDefinitions);
        }
        final DataObject selectedValues = DataObject.newArrayData(objectDefinitions);

        final DataObject accessParameter = DataObject.newStructureData(Arrays.asList(clockDefinition, fromValue,
                toValue, selectedValues));

        return new SelectiveAccessDescription(accessSelector, accessParameter);
    }

    private void addSelectedValues(final Channel channel, final PeriodType periodType,
            final List<DataObject> objectDefinitions) {

        switch (periodType) {
        case INTERVAL:
            // empty objectDefinitions is ok, since all values are applicable,
            // hence selective access is not applicable
            break;
        case DAILY:
            this.addSelectedValuesForDaily(objectDefinitions, channel);
            break;
        case MONTHLY:
            this.addSelectedValuesForMonthly(objectDefinitions, channel);
            break;
        default:
            throw new AssertionError("Unknown PeriodType: " + periodType);
        }
    }

    private void addSelectedValuesForMonthly(final List<DataObject> objectDefinitions, final Channel channel) {
        /*-
         * Available objects in the profile buffer (0-0:98.1.0.255):
         * {8,0-0:1.0.0.255,2,0}    -  clock
         *
         * Objects not retrieved with G meter readings
         * {3,1-0:1.8.1.255,2,0}    -  Active energy import (+A) rate 1
         * {3,1-0:1.8.2.255,2,0}    -  Active energy import (+A) rate 2
         * {3,1-0:2.8.1.255,2,0}    -  Active energy export (-A) rate 1
         * {3,1-0:2.8.2.255,2,0}    -  Active energy export (-A) rate 2
         *
         * Objects retrieved depending on the channel value passed in:
         * {4,0-1.24.2.1.255,2,0}  -  M-Bus Master Value 1 Channel 1
         * {4,0-1.24.2.1.255,5,0}  -  M-Bus Master Value 1 Channel 1 Capture time
         * {4,0-2.24.2.1.255,2,0}  -  M-Bus Master Value 1 Channel 2
         * {4,0-2.24.2.1.255,5,0}  -  M-Bus Master Value 1 Channel 2 Capture time
         * {4,0-3.24.2.1.255,2,0}  -  M-Bus Master Value 1 Channel 3
         * {4,0-3.24.2.1.255,5,0}  -  M-Bus Master Value 1 Channel 3 Capture time
         * {4,0-4.24.2.1.255,2,0}  -  M-Bus Master Value 1 Channel 4
         * {4,0-4.24.2.1.255,5,0}  -  M-Bus Master Value 1 Channel 4 Capture time
         */

        objectDefinitions.add(this.dlmsHelperService.getClockDefinition());

        this.addMBusMasterValue1(objectDefinitions, channel);
        this.addMBusMasterValue1CaptureTime(objectDefinitions, channel);
    }

    private void addSelectedValuesForDaily(final List<DataObject> objectDefinitions, final Channel channel) {
        /*-
         * Available objects in the profile buffer (1-0:99.2.0.255):
         * {8,0-0:1.0.0.255,2,0}    -  clock
         * {1,0-0:96.10.2.255,2,0}  -  AMR profile status
         *
         * Objects not retrieved with G meter readings
         * {3,1-0:1.8.1.255,2,0}    -  Active energy import (+A) rate 1
         * {3,1-0:1.8.2.255,2,0}    -  Active energy import (+A) rate 2
         * {3,1-0:2.8.1.255,2,0}    -  Active energy export (-A) rate 1
         * {3,1-0:2.8.2.255,2,0}    -  Active energy export (-A) rate 2
         *
         * Objects retrieved depending on the channel value passed in:
         * {4,0-1.24.2.1.255,2,0}  -  M-Bus Master Value 1 Channel 1
         * {4,0-1.24.2.1.255,5,0}  -  M-Bus Master Value 1 Channel 1 Capture time
         * {4,0-2.24.2.1.255,2,0}  -  M-Bus Master Value 1 Channel 2
         * {4,0-2.24.2.1.255,5,0}  -  M-Bus Master Value 1 Channel 2 Capture time
         * {4,0-3.24.2.1.255,2,0}  -  M-Bus Master Value 1 Channel 3
         * {4,0-3.24.2.1.255,5,0}  -  M-Bus Master Value 1 Channel 3 Capture time
         * {4,0-4.24.2.1.255,2,0}  -  M-Bus Master Value 1 Channel 4
         * {4,0-4.24.2.1.255,5,0}  -  M-Bus Master Value 1 Channel 4 Capture time
         */

        objectDefinitions.add(this.dlmsHelperService.getClockDefinition());

        objectDefinitions.add(this.dlmsHelperService.getAMRProfileDefinition());

        this.addMBusMasterValue1(objectDefinitions, channel);
        this.addMBusMasterValue1CaptureTime(objectDefinitions, channel);
    }

    /**
     * Reads AmrProfileStatusCode from DataObject holding a bitvalue in a
     * numeric datatype.
     *
     * @param amrProfileStatusData
     *            AMR profile register value.
     * @return AmrProfileStatusCode object holding status enum values.
     * @throws ProtocolAdapterException
     *             on invalid register data.
     */
    private AmrProfileStatusCode readAmrProfileStatusCode(final DataObject amrProfileStatusData)
            throws ProtocolAdapterException {
        AmrProfileStatusCode amrProfileStatusCode = null;

        if (!amrProfileStatusData.isNumber()) {
            throw new ProtocolAdapterException("Could not read AMR profile register data. Invalid data type.");
        }

        final Set<AmrProfileStatusCodeFlag> flags = this.amrProfileStatusCodeHelperService
                .toAmrProfileStatusCodeFlags((Number) amrProfileStatusData.value());
        amrProfileStatusCode = new AmrProfileStatusCode(flags);

        return amrProfileStatusCode;
    }

    private void addMBusMasterValue1(final List<DataObject> objectDefinitions, final Channel channel) {
        // {4,0-x.24.2.1.255,2,0} - M-Bus Master Value 1 Channel x
        // where x is the channel
        objectDefinitions.add(DataObject.newStructureData(Arrays.asList(DataObject.newUInteger16Data(CLASS_ID_MBUS),
                DataObject.newOctetStringData(OBIS_BYTES_M_BUS_MASTER_VALUE_1_CHANNEL_MAP.get(channel
                        .getChannelNumber())), DataObject.newInteger8Data(ATTRIBUTE_M_BUS_MASTER_VALUE), DataObject
                        .newUInteger16Data(0))));
    }

    private void addMBusMasterValue1CaptureTime(final List<DataObject> objectDefinitions, final Channel channel) {
        // {4,0-x.24.2.1.255,2,0} - M-Bus Master Value 1 Channel x
        // where x is the channel
        objectDefinitions.add(DataObject.newStructureData(Arrays.asList(DataObject.newUInteger16Data(CLASS_ID_MBUS),
                DataObject.newOctetStringData(OBIS_BYTES_M_BUS_MASTER_VALUE_1_CHANNEL_MAP.get(channel
                        .getChannelNumber())), DataObject.newInteger8Data(ATTRIBUTE_M_BUS_MASTER_VALUE_CAPTURE_TIME),
                        DataObject.newUInteger16Data(0))));
    }

}
