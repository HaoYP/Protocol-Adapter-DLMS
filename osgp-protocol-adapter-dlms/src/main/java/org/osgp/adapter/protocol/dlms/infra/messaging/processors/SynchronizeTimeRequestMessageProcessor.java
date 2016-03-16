/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.infra.messaging.processors;

import java.io.Serializable;

import org.osgp.adapter.protocol.dlms.application.services.AdhocService;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.osgp.adapter.protocol.dlms.infra.messaging.DeviceRequestMessageProcessor;
import org.osgp.adapter.protocol.dlms.infra.messaging.DeviceRequestMessageType;
import org.osgp.adapter.protocol.dlms.infra.messaging.DlmsDeviceMessageMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alliander.osgp.dto.valueobjects.smartmetering.SynchronizeTimeRequest;
import com.alliander.osgp.shared.exceptionhandling.OsgpException;

/**
 * Class for processing Synchronize Time Request messages
 */
@Component("dlmsSynchronizeTimeRequestMessageProcessor")
public class SynchronizeTimeRequestMessageProcessor extends DeviceRequestMessageProcessor {

    @Autowired
    private AdhocService adhocService;

    public SynchronizeTimeRequestMessageProcessor() {
        super(DeviceRequestMessageType.SYNCHRONIZE_TIME);
    }

    @Override
    protected Serializable handleMessage(final DlmsDeviceMessageMetadata messageMetadata,
            final Serializable requestObject) throws OsgpException, ProtocolAdapterException {
        final SynchronizeTimeRequest synchronizeTimeRequest = (SynchronizeTimeRequest) requestObject;
        this.adhocService.synchronizeTime(messageMetadata, synchronizeTimeRequest);
        return null;
    }
}
