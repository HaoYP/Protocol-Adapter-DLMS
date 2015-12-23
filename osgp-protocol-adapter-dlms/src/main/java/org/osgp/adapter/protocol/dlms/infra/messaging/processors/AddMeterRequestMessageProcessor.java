/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.infra.messaging.processors;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.osgp.adapter.protocol.dlms.application.services.InstallationService;
import org.osgp.adapter.protocol.dlms.infra.messaging.DeviceRequestMessageProcessor;
import org.osgp.adapter.protocol.dlms.infra.messaging.DeviceRequestMessageType;
import org.osgp.adapter.protocol.dlms.infra.messaging.DlmsDeviceMessageMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alliander.osgp.dto.valueobjects.smartmetering.SmartMeteringDevice;

/**
 * Class for processing add meter request messages
 */
@Component("dlmsAddMeterRequestMessageProcessor")
public class AddMeterRequestMessageProcessor extends DeviceRequestMessageProcessor {
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AddMeterRequestMessageProcessor.class);

    @Autowired
    private InstallationService installationService;

    public AddMeterRequestMessageProcessor() {
        super(DeviceRequestMessageType.ADD_METER);
    }

    @Override
    public void processMessage(final ObjectMessage message) {
        LOGGER.debug("Processing add meter request message");

        final DlmsDeviceMessageMetadata messageMetadata = new DlmsDeviceMessageMetadata();

        try {
            messageMetadata.handleMessage(message);
            final SmartMeteringDevice smartMeteringDevice = (SmartMeteringDevice) message.getObject();

            this.installationService.addMeter(messageMetadata, smartMeteringDevice, this.responseMessageSender);

        } catch (final JMSException exception) {
            this.logJmsException(LOGGER, exception, messageMetadata);
        }
    }
}
