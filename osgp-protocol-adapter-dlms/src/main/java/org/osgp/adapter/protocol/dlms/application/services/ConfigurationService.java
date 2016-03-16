/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.application.services;

import java.util.List;

import org.openmuc.jdlms.AccessResultCode;
import org.openmuc.jdlms.LnClientConnection;
import org.openmuc.jdlms.MethodResultCode;
import org.openmuc.jdlms.SecurityUtils.KeyId;
import org.osgp.adapter.protocol.dlms.application.models.ProtocolMeterInfo;
import org.osgp.adapter.protocol.dlms.domain.commands.GetAdministrativeStatusCommandExecutor;
import org.osgp.adapter.protocol.dlms.domain.commands.GetFirmwareVersionCommandExecutor;
import org.osgp.adapter.protocol.dlms.domain.commands.GetPushSetupSmsCommandExecutor;
import org.osgp.adapter.protocol.dlms.domain.commands.ReplaceKeyCommandExecutor;
import org.osgp.adapter.protocol.dlms.domain.commands.SetActivityCalendarCommandActivationExecutor;
import org.osgp.adapter.protocol.dlms.domain.commands.SetActivityCalendarCommandExecutor;
import org.osgp.adapter.protocol.dlms.domain.commands.SetAdministrativeStatusCommandExecutor;
import org.osgp.adapter.protocol.dlms.domain.commands.SetAlarmNotificationsCommandExecutor;
import org.osgp.adapter.protocol.dlms.domain.commands.SetConfigurationObjectCommandExecutor;
import org.osgp.adapter.protocol.dlms.domain.commands.SetEncryptionKeyExchangeOnGMeterCommandExecutor;
import org.osgp.adapter.protocol.dlms.domain.commands.SetPushSetupAlarmCommandExecutor;
import org.osgp.adapter.protocol.dlms.domain.commands.SetPushSetupSmsCommandExecutor;
import org.osgp.adapter.protocol.dlms.domain.commands.SetSpecialDaysCommandExecutor;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.domain.entities.SecurityKeyType;
import org.osgp.adapter.protocol.dlms.domain.factories.DlmsConnectionFactory;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.osgp.adapter.protocol.dlms.infra.messaging.DlmsDeviceMessageMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alliander.osgp.dto.valueobjects.smartmetering.ActivityCalendar;
import com.alliander.osgp.dto.valueobjects.smartmetering.AdministrativeStatusType;
import com.alliander.osgp.dto.valueobjects.smartmetering.AlarmNotifications;
import com.alliander.osgp.dto.valueobjects.smartmetering.ConfigurationFlag;
import com.alliander.osgp.dto.valueobjects.smartmetering.ConfigurationFlags;
import com.alliander.osgp.dto.valueobjects.smartmetering.ConfigurationObject;
import com.alliander.osgp.dto.valueobjects.smartmetering.GMeterInfo;
import com.alliander.osgp.dto.valueobjects.smartmetering.GprsOperationModeType;
import com.alliander.osgp.dto.valueobjects.smartmetering.KeySet;
import com.alliander.osgp.dto.valueobjects.smartmetering.PushSetupAlarm;
import com.alliander.osgp.dto.valueobjects.smartmetering.PushSetupSms;
import com.alliander.osgp.dto.valueobjects.smartmetering.SetConfigurationObjectRequest;
import com.alliander.osgp.dto.valueobjects.smartmetering.SpecialDay;
import com.alliander.osgp.dto.valueobjects.smartmetering.SpecialDaysRequest;
import com.alliander.osgp.dto.valueobjects.smartmetering.SpecialDaysRequestData;
import com.alliander.osgp.shared.exceptionhandling.OsgpException;

@Service(value = "dlmsConfigurationService")
public class ConfigurationService {
    private static final String VISUAL_SEPARATOR = "******************************************************";

    private static final String DEBUG_MSG_CLOSING_CONNECTION = "Closing connection with {}";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationService.class);

    @Autowired
    private DomainHelperService domainHelperService;

    @Autowired
    private DlmsConnectionFactory dlmsConnectionFactory;

    @Autowired
    private SetSpecialDaysCommandExecutor setSpecialDaysCommandExecutor;

    @Autowired
    private SetAlarmNotificationsCommandExecutor setAlarmNotificationsCommandExecutor;

    @Autowired
    private SetConfigurationObjectCommandExecutor setConfigurationObjectCommandExecutor;

    @Autowired
    private SetPushSetupAlarmCommandExecutor setPushSetupAlarmCommandExecutor;

    @Autowired
    private GetPushSetupSmsCommandExecutor getPushSetupSmsCommandExecutor;

    @Autowired
    private SetPushSetupSmsCommandExecutor setPushSetupSmsCommandExecutor;

    @Autowired
    private SetActivityCalendarCommandExecutor setActivityCalendarCommandExecutor;

    @Autowired
    private SetEncryptionKeyExchangeOnGMeterCommandExecutor setEncryptionKeyExchangeOnGMeterCommandExecutor;

    @Autowired
    private SetActivityCalendarCommandActivationExecutor setActivityCalendarCommandActivationExecutor;

    @Autowired
    private SetAdministrativeStatusCommandExecutor setAdministrativeStatusCommandExecutor;

    @Autowired
    private GetAdministrativeStatusCommandExecutor getAdministrativeStatusCommandExecutor;

    @Autowired
    private GetFirmwareVersionCommandExecutor getFirmwareVersionCommandExecutor;

    @Autowired
    private ReplaceKeyCommandExecutor replaceKeyCommandExecutor;

    public void requestSpecialDays(final DlmsDeviceMessageMetadata messageMetadata,
            final SpecialDaysRequest specialDaysRequest) throws OsgpException, ProtocolAdapterException {

        LnClientConnection conn = null;
        try {
            // The Special days towards the Smart Meter
            final SpecialDaysRequestData specialDaysRequestData = specialDaysRequest.getSpecialDaysRequestData();

            LOGGER.info(VISUAL_SEPARATOR);
            LOGGER.info("********** Set Special Days: 0-0:11.0.0.255 **********");
            LOGGER.info(VISUAL_SEPARATOR);
            final List<SpecialDay> specialDays = specialDaysRequestData.getSpecialDays();
            for (final SpecialDay specialDay : specialDays) {
                LOGGER.info("Date :{}, dayId : {} ", specialDay.getSpecialDayDate(), specialDay.getDayId());
            }
            LOGGER.info(VISUAL_SEPARATOR);

            final DlmsDevice device = this.domainHelperService.findDlmsDevice(messageMetadata);
            conn = this.dlmsConnectionFactory.getConnection(device);

            final AccessResultCode accessResultCode = this.setSpecialDaysCommandExecutor.execute(conn, device,
                    specialDays);
            if (!AccessResultCode.SUCCESS.equals(accessResultCode)) {
                throw new ProtocolAdapterException("Set special days reported result is: " + accessResultCode);
            }
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    // === REQUEST Configuration Object DATA ===

    public void requestSetConfiguration(final DlmsDeviceMessageMetadata messageMetadata,
            final SetConfigurationObjectRequest setConfigurationObjectRequest) throws OsgpException,
            ProtocolAdapterException {

        LnClientConnection conn = null;
        try {
            // Configuration Object towards the Smart Meter
            final ConfigurationObject configurationObject = setConfigurationObjectRequest
                    .getSetConfigurationObjectRequestData().getConfigurationObject();

            final GprsOperationModeType gprsOperationModeType = configurationObject.getGprsOperationMode();
            final ConfigurationFlags configurationFlags = configurationObject.getConfigurationFlags();

            LOGGER.info(VISUAL_SEPARATOR);
            LOGGER.info("******** Configuration Object: 0-0:94.31.3.255 *******");
            LOGGER.info(VISUAL_SEPARATOR);
            LOGGER.info("Operation mode:{} ", gprsOperationModeType.value());
            LOGGER.info("Flags:");

            for (final ConfigurationFlag configurationFlag : configurationFlags.getConfigurationFlag()) {
                LOGGER.info("Flag : {}, enabled = {}", configurationFlag.getConfigurationFlagType().toString(),
                        configurationFlag.isEnabled());
            }
            LOGGER.info(VISUAL_SEPARATOR);

            final DlmsDevice device = this.domainHelperService.findDlmsDevice(messageMetadata);
            conn = this.dlmsConnectionFactory.getConnection(device);

            final AccessResultCode accessResultCode = this.setConfigurationObjectCommandExecutor.execute(conn, device,
                    configurationObject);
            if (!AccessResultCode.SUCCESS.equals(accessResultCode)) {
                throw new ProtocolAdapterException("Set configuration object reported result is: " + accessResultCode);
            }

        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    public void requestSetAdministrativeStatus(final DlmsDeviceMessageMetadata messageMetadata,
            final AdministrativeStatusType administrativeStatusType) throws OsgpException, ProtocolAdapterException {

        LnClientConnection conn = null;
        DlmsDevice device = null;
        try {
            device = this.domainHelperService.findDlmsDevice(messageMetadata);

            LOGGER.info("Device for Set Administrative Status is: {}", device);

            conn = this.dlmsConnectionFactory.getConnection(device);
            this.setAdministrativeStatusCommandExecutor.execute(conn, device, administrativeStatusType);

            final AccessResultCode accessResultCode = this.setAdministrativeStatusCommandExecutor.execute(conn, device,
                    administrativeStatusType);
            if (AccessResultCode.SUCCESS != accessResultCode) {
                throw new ProtocolAdapterException("AccessResultCode for set administrative status was not SUCCESS: "
                        + accessResultCode);
            }

        } finally {
            if (conn != null) {
                LOGGER.info(DEBUG_MSG_CLOSING_CONNECTION, device.getDeviceIdentification());
                conn.close();
            }
        }
    }

    public void setAlarmNotifications(final DlmsDeviceMessageMetadata messageMetadata,
            final AlarmNotifications alarmNotifications) throws OsgpException, ProtocolAdapterException {

        LnClientConnection conn = null;
        try {

            LOGGER.info("Alarm Notifications to set on the device: {}", alarmNotifications);

            final DlmsDevice device = this.domainHelperService.findDlmsDevice(messageMetadata);

            conn = this.dlmsConnectionFactory.getConnection(device);

            final AccessResultCode accessResultCode = this.setAlarmNotificationsCommandExecutor.execute(conn, device,
                    alarmNotifications);
            if (AccessResultCode.SUCCESS != accessResultCode) {
                throw new ProtocolAdapterException("AccessResultCode for set alarm notifications was not SUCCESS: "
                        + accessResultCode);
            }
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    public AdministrativeStatusType requestGetAdministrativeStatus(final DlmsDeviceMessageMetadata messageMetadata)
            throws OsgpException, ProtocolAdapterException {

        LnClientConnection conn = null;
        try {
            final DlmsDevice device = this.domainHelperService.findDlmsDevice(messageMetadata);

            conn = this.dlmsConnectionFactory.getConnection(device);

            return this.getAdministrativeStatusCommandExecutor.execute(conn, device, null);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    public String setEncryptionKeyExchangeOnGMeter(final DlmsDeviceMessageMetadata messageMetadata,
            final GMeterInfo gMeterInfo) throws OsgpException, ProtocolAdapterException {

        LnClientConnection conn = null;
        DlmsDevice device = null;
        try {
            final String deviceIdentification = messageMetadata.getDeviceIdentification();
            device = this.domainHelperService.findDlmsDevice(messageMetadata);

            LOGGER.info("Device for Set Encryption Key Exchange On G-Meter is: {}", device);

            conn = this.dlmsConnectionFactory.getConnection(device);

            // Get G-Meter
            final DlmsDevice gMeterDevice = this.domainHelperService.findDlmsDevice(gMeterInfo
                    .getDeviceIdentification());
            final ProtocolMeterInfo protocolMeterInfo = new ProtocolMeterInfo(gMeterInfo.getChannel(),
                    gMeterInfo.getDeviceIdentification(), gMeterDevice.getValidSecurityKey(
                            SecurityKeyType.G_METER_ENCRYPTION).getKey(), gMeterDevice.getValidSecurityKey(
                            SecurityKeyType.G_METER_MASTER).getKey());

            this.setEncryptionKeyExchangeOnGMeterCommandExecutor.execute(conn, device, protocolMeterInfo);

            return "Set Encryption Key Exchange On G-Meter Result is OK for device id: " + deviceIdentification;
        } finally {
            if (conn != null) {
                LOGGER.info(DEBUG_MSG_CLOSING_CONNECTION, device.getDeviceIdentification());
                conn.close();
            }
        }
    }

    public String setActivityCalendar(final DlmsDeviceMessageMetadata messageMetadata,
            final ActivityCalendar activityCalendar) throws OsgpException, ProtocolAdapterException {

        LnClientConnection conn = null;
        DlmsDevice device = null;
        try {
            final String deviceIdentification = messageMetadata.getDeviceIdentification();
            device = this.domainHelperService.findDlmsDevice(messageMetadata);

            LOGGER.info("Device for Activity Calendar is: {}", device);

            conn = this.dlmsConnectionFactory.getConnection(device);
            this.setActivityCalendarCommandExecutor.execute(conn, device, activityCalendar);

            final MethodResultCode methodResult = this.setActivityCalendarCommandActivationExecutor.execute(conn,
                    device, null);

            if (!MethodResultCode.SUCCESS.equals(methodResult)) {
                throw new ProtocolAdapterException("AccessResultCode for set Activity Calendar: " + methodResult);
            }

            return "Set Activity Calendar Result is OK for device id: " + deviceIdentification + " calendar name: "
                    + activityCalendar.getCalendarName();
        } finally {
            if (conn != null) {
                LOGGER.info(DEBUG_MSG_CLOSING_CONNECTION, device.getDeviceIdentification());
                conn.close();
            }
        }

    }

    public void setPushSetupAlarm(final DlmsDeviceMessageMetadata messageMetadata, final PushSetupAlarm pushSetupAlarm)
            throws OsgpException, ProtocolAdapterException {

        LnClientConnection conn = null;
        try {
            LOGGER.info("Push Setup Alarm to set on the device: {}", pushSetupAlarm);

            final DlmsDevice device = this.domainHelperService.findDlmsDevice(messageMetadata);

            conn = this.dlmsConnectionFactory.getConnection(device);

            final AccessResultCode accessResultCode = this.setPushSetupAlarmCommandExecutor.execute(conn, device,
                    pushSetupAlarm);

            if (AccessResultCode.SUCCESS != accessResultCode) {
                throw new ProtocolAdapterException("AccessResultCode for set push setup alarm was not SUCCESS: "
                        + accessResultCode);
            }
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    public void setPushSetupSms(final DlmsDeviceMessageMetadata messageMetadata, final PushSetupSms pushSetupSms)
            throws OsgpException, ProtocolAdapterException {

        LnClientConnection conn = null;
        try {
            LOGGER.info("Push Setup Sms to set on the device: {}", pushSetupSms);

            final DlmsDevice device = this.domainHelperService.findDlmsDevice(messageMetadata);

            conn = this.dlmsConnectionFactory.getConnection(device);

            final AccessResultCode accessResultCode = this.setPushSetupSmsCommandExecutor.execute(conn, device,
                    pushSetupSms);

            if (AccessResultCode.SUCCESS != accessResultCode) {
                throw new ProtocolAdapterException("AccessResultCode for set push setup sms was not SUCCESS: "
                        + accessResultCode);
            }

        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    public String requestFirmwareVersion(final DlmsDeviceMessageMetadata messageMetadata) throws OsgpException,
    ProtocolAdapterException {

        LnClientConnection conn = null;
        try {
            final DlmsDevice device = this.domainHelperService.findDlmsDevice(messageMetadata);
            conn = this.dlmsConnectionFactory.getConnection(device);

            return this.getFirmwareVersionCommandExecutor.execute(conn, device, null);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    public void replaceKeys(final DlmsDeviceMessageMetadata messageMetadata, final KeySet keySet) throws OsgpException,
            ProtocolAdapterException {

        LnClientConnection conn = null;

        try {
            final DlmsDevice device = this.domainHelperService.findDlmsDevice(messageMetadata);
            conn = this.dlmsConnectionFactory.getConnection(device);

            this.replaceKeySet(conn, device, keySet);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    private void replaceKeySet(final LnClientConnection conn, final DlmsDevice device, final KeySet keySet)
            throws ProtocolAdapterException {

        try {
            // Change AUTHENTICATION key.
            LOGGER.info("Keys to set on the device {}: {}", device.getDeviceIdentification(), keySet);
            DlmsDevice devicePostSave = this.replaceKeyCommandExecutor.execute(conn, device, ReplaceKeyCommandExecutor
                    .wrap(keySet.getAuthenticationKey(), KeyId.AUTHENTICATION_KEY,
                            SecurityKeyType.E_METER_AUTHENTICATION));
            conn.changeClientGlobalAuthenticationKey(keySet.getAuthenticationKey());

            // Change ENCRYPTION key
            devicePostSave = this.replaceKeyCommandExecutor.execute(conn, devicePostSave, ReplaceKeyCommandExecutor
                    .wrap(keySet.getEncryptionKey(), KeyId.GLOBAL_UNICAST_ENCRYPTION_KEY,
                            SecurityKeyType.E_METER_ENCRYPTION));
            conn.changeClientGlobalEncryptionKey(keySet.getEncryptionKey());
        } catch (final ProtocolAdapterException e) {
            LOGGER.error("Unexpected exception during replaceKeys.", e);
            throw e;
        }
    }

}
