/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.infra.messaging;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.openmuc.jdlms.ClientConnection;
import org.osgp.adapter.protocol.dlms.application.jasper.sessionproviders.exceptions.SessionProviderException;
import org.osgp.adapter.protocol.dlms.application.services.DomainHelperService;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.domain.factories.DlmsConnectionFactory;
import org.osgp.adapter.protocol.dlms.exceptions.ConnectionException;
import org.osgp.adapter.protocol.dlms.exceptions.OsgpExceptionConverter;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.alliander.osgp.shared.exceptionhandling.OsgpException;
import com.alliander.osgp.shared.infra.jms.DeviceMessageMetadata;
import com.alliander.osgp.shared.infra.jms.MessageProcessor;
import com.alliander.osgp.shared.infra.jms.MessageProcessorMap;
import com.alliander.osgp.shared.infra.jms.ProtocolResponseMessage;
import com.alliander.osgp.shared.infra.jms.ResponseMessageResultType;

/**
 * Base class for MessageProcessor implementations. Each MessageProcessor
 * implementation should be annotated with @Component. Further the MessageType
 * the MessageProcessor implementation can process should be passed in at
 * construction. The Singleton instance is added to the HashMap of
 * MessageProcessors after dependency injection has completed.
 */
public abstract class DeviceRequestMessageProcessor implements MessageProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceRequestMessageProcessor.class);

    @Autowired
    protected DeviceResponseMessageSender responseMessageSender;

    @Autowired
    @Qualifier("protocolDlmsDeviceRequestMessageProcessorMap")
    protected MessageProcessorMap dlmsRequestMessageProcessorMap;

    @Autowired
    protected OsgpExceptionConverter osgpExceptionConverter;

    @Autowired
    private DomainHelperService domainHelperService;

    @Autowired
    private DlmsConnectionFactory dlmsConnectionFactory;

    protected final DeviceRequestMessageType deviceRequestMessageType;

    /**
     * Each MessageProcessor should register it's MessageType at construction.
     *
     * @param deviceRequestMessageType
     *            The MessageType the MessageProcessor implementation can
     *            process.
     */
    protected DeviceRequestMessageProcessor(final DeviceRequestMessageType deviceRequestMessageType) {
        this.deviceRequestMessageType = deviceRequestMessageType;
    }

    /**
     * Initialization function executed after dependency injection has finished.
     * The MessageProcessor Singleton is added to the HashMap of
     * MessageProcessors. The key for the HashMap is the integer value of the
     * enumeration member.
     */
    @PostConstruct
    public void init() {
        this.dlmsRequestMessageProcessorMap.addMessageProcessor(this.deviceRequestMessageType.ordinal(),
                this.deviceRequestMessageType.name(), this);
    }

    /**
     * @param logger
     *            the logger from the calling subClass
     * @param exception
     *            the exception to be logged
     * @param messageMetadata
     *            a DlmsDeviceMessageMetadata containing debug info to be logged
     */
    private void logJmsException(final Logger logger, final JMSException exception,
            final DlmsDeviceMessageMetadata messageMetadata) {
        logger.error("UNRECOVERABLE ERROR, unable to read ObjectMessage instance, giving up.", exception);
        logger.debug("correlationUid: {}", messageMetadata.getCorrelationUid());
        logger.debug("domain: {}", messageMetadata.getDomain());
        logger.debug("domainVersion: {}", messageMetadata.getDomainVersion());
        logger.debug("messageType: {}", messageMetadata.getMessageType());
        logger.debug("organisationIdentification: {}", messageMetadata.getOrganisationIdentification());
        logger.debug("deviceIdentification: {}", messageMetadata.getDeviceIdentification());
    }

    @Override
    public void processMessage(final ObjectMessage message) throws JMSException {
        LOGGER.debug("Processing {} request message", this.deviceRequestMessageType.name());
        final DlmsDeviceMessageMetadata messageMetadata = new DlmsDeviceMessageMetadata();

        ClientConnection conn = null;
        DlmsDevice device = null;

        try {
            // Handle message
            messageMetadata.handleMessage(message);

            LOGGER.info("{} called for device: {} for organisation: {}", message.getJMSType(),
                    messageMetadata.getDeviceIdentification(), messageMetadata.getOrganisationIdentification());

            device = this.domainHelperService.findDlmsDevice(messageMetadata);
            conn = this.dlmsConnectionFactory.getConnection(device);

            final Serializable response = this.handleMessage(conn, device, message.getObject());

            // Send response
            this.sendResponseMessage(messageMetadata, ResponseMessageResultType.OK, null, this.responseMessageSender,
                    response);
        } catch (final ConnectionException exception) {
            // Retry / redeliver by throwing RuntimeException.
            LOGGER.info("ConnectionException occurred, JMS will catch this exception.");
            throw exception;
        } catch (final JMSException exception) {
            this.logJmsException(LOGGER, exception, messageMetadata);
        } catch (final Exception exception) {
            // Return original request + exception
            LOGGER.error("Unexpected exception during {}", this.deviceRequestMessageType.name(), exception);

            final OsgpException ex = this.osgpExceptionConverter.ensureOsgpOrTechnicalException(exception);
            this.sendResponseMessage(messageMetadata, ResponseMessageResultType.NOT_OK, ex, this.responseMessageSender,
                    message.getObject());
        } finally {
            if (conn != null) {
                LOGGER.info("Closing connection with {}", device.getDeviceIdentification());
                conn.close();
            }
        }
    }

    /**
     * Implementation of this method should call a service that can handle the
     * requestObject and return a response object to be put on the response
     * queue. This response object can also be null for methods that don't
     * provide result data.
     *
     * @param ClientConnection
     *            the connection to the device.
     * @param device
     *            the device.
     * @param requestObject
     *            Request data object.
     * @return A serializable object to be put on the response queue.
     * @throws OsgpException
     * @throws ProtocolAdapterException
     * @throws SessionProviderException
     */
    protected abstract Serializable handleMessage(ClientConnection conn, final DlmsDevice device,
            final Serializable requestObject) throws OsgpException, ProtocolAdapterException, SessionProviderException;

    private void sendResponseMessage(final DlmsDeviceMessageMetadata dlmsDeviceMessageMetadata,
            final ResponseMessageResultType result, final OsgpException osgpException,
            final DeviceResponseMessageSender responseMessageSender, final Serializable responseObject) {

        final DeviceMessageMetadata deviceMessageMetadata = dlmsDeviceMessageMetadata.asDeviceMessageMetadata();

        // @formatter:off
        final ProtocolResponseMessage responseMessage = new ProtocolResponseMessage.Builder()
        .deviceMessageMetadata(deviceMessageMetadata)
        .domain(dlmsDeviceMessageMetadata.getDomain())
        .domainVersion(dlmsDeviceMessageMetadata.getDomainVersion())
        .result(result)
        .osgpException(osgpException)
        .dataObject(responseObject)
        .retryCount(dlmsDeviceMessageMetadata.getRetryCount())
        .build();
        // @formatter:on

        responseMessageSender.send(responseMessage);
    }
}
