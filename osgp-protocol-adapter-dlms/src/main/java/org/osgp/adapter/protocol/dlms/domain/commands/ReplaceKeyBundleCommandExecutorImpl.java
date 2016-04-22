/**
 * Copyright 2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.commands;

import org.openmuc.jdlms.ClientConnection;
import org.openmuc.jdlms.SecurityUtils.KeyId;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.domain.entities.SecurityKeyType;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alliander.osgp.dto.valueobjects.smartmetering.ActionResponseDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.KeySetDto;

@Component
public class ReplaceKeyBundleCommandExecutorImpl implements ReplaceKeyBundleCommandExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReplaceKeyBundleCommandExecutorImpl.class);

    @Autowired
    ReplaceKeyCommandExecutor replaceKeyCommandExecutor;

    @Override
    public ActionResponseDto execute(final ClientConnection conn, final DlmsDevice device,
            final KeySetDto keySetDto) {

        // Add the // Change AUTHENTICATION key.
        LOGGER.info("Keys to set on the device {}: {}", device.getDeviceIdentification(), keySetDto);
        DlmsDevice devicePostSave;
        try {
            devicePostSave = this.replaceKeyCommandExecutor
                    .execute(conn, device, ReplaceKeyCommandExecutor.wrap(keySetDto.getAuthenticationKey(),
                            KeyId.AUTHENTICATION_KEY, SecurityKeyType.E_METER_AUTHENTICATION));
            conn.changeClientGlobalAuthenticationKey(keySetDto.getAuthenticationKey());

            // Change ENCRYPTION key
            devicePostSave = this.replaceKeyCommandExecutor.execute(conn, devicePostSave, ReplaceKeyCommandExecutor
                    .wrap(keySetDto.getEncryptionKey(), KeyId.GLOBAL_UNICAST_ENCRYPTION_KEY,
                            SecurityKeyType.E_METER_ENCRYPTION));
            conn.changeClientGlobalEncryptionKey(keySetDto.getEncryptionKey());
        } catch (final ProtocolAdapterException e) {
            LOGGER.error("Replace keys for device: " + device.getDeviceIdentification() + " was not successful");
            return new ActionResponseDto(e, "Replace keys for device: " + device.getDeviceIdentification()
                    + " was not successful");
        }

        return new ActionResponseDto("Replace keys for device: " + device.getDeviceIdentification()
                + " was successful");
    }
}