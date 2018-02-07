/**
 * Copyright 2017 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.infra.messaging.processors;

import java.io.Serializable;

import org.osgp.adapter.protocol.dlms.application.services.InstallationService;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.domain.factories.DlmsConnectionHolder;
import org.osgp.adapter.protocol.dlms.infra.messaging.DeviceRequestMessageProcessor;
import org.osgp.adapter.protocol.dlms.infra.messaging.DeviceRequestMessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alliander.osgp.dto.valueobjects.smartmetering.DeCoupleMbusDeviceDto;
import com.alliander.osgp.shared.exceptionhandling.OsgpException;

@Component
public class DeCoupleMbusDeviceRequestMessageProcessor extends DeviceRequestMessageProcessor {

    @Autowired
    private InstallationService installationService;

    protected DeCoupleMbusDeviceRequestMessageProcessor() {
        super(DeviceRequestMessageType.DE_COUPLE_MBUS_DEVICE);
    }

    @Override
    protected Serializable handleMessage(final DlmsConnectionHolder conn, final DlmsDevice device,
            final Serializable requestObject) throws OsgpException {

        this.assertRequestObjectType(DeCoupleMbusDeviceDto.class, requestObject);

        final DeCoupleMbusDeviceDto deCoupleMbusDeviceDto = (DeCoupleMbusDeviceDto) requestObject;
        return this.installationService.deCoupleMbusDevice(conn, device, deCoupleMbusDeviceDto);
    }
}
