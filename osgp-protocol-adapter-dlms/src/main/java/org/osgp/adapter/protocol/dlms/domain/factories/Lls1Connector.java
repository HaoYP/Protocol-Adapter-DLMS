/**
 * Copyright 2017 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.factories;

import java.io.IOException;
import java.net.UnknownHostException;

import org.openmuc.jdlms.AuthenticationMechanism;
import org.openmuc.jdlms.DlmsConnection;
import org.openmuc.jdlms.SecuritySuite;
import org.openmuc.jdlms.TcpConnectionBuilder;
import org.osgp.adapter.protocol.dlms.application.services.SecurityKeyService;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.exceptions.ConnectionException;
import org.osgp.adapter.protocol.dlms.infra.messaging.DlmsMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.alliander.osgp.shared.exceptionhandling.ComponentType;
import com.alliander.osgp.shared.exceptionhandling.EncrypterException;
import com.alliander.osgp.shared.exceptionhandling.FunctionalException;
import com.alliander.osgp.shared.exceptionhandling.FunctionalExceptionType;
import com.alliander.osgp.shared.exceptionhandling.OsgpException;
import com.alliander.osgp.shared.exceptionhandling.TechnicalException;

public class Lls1Connector extends SecureDlmsConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(Lls1Connector.class);

    @Autowired
    private SecurityKeyService securityKeyService;

    public Lls1Connector(final int responseTimeout, final int logicalDeviceAddress, final int clientAccessPoint) {
        super(responseTimeout, logicalDeviceAddress, clientAccessPoint);
    }

    @Override
    public DlmsConnection connect(final DlmsDevice device, final DlmsMessageListener dlmsMessageListener)
            throws OsgpException {

        // Make sure neither device or device.getIpAddress() is null.
        this.checkDevice(device);
        this.checkIpAddress(device);

        try {
            return this.createConnection(device, dlmsMessageListener);
        } catch (final UnknownHostException e) {
            LOGGER.warn("The IP address is not found: {}", device.getIpAddress(), e);
            // Unknown IP, unrecoverable.
            throw new TechnicalException(ComponentType.PROTOCOL_DLMS,
                    "The IP address is not found: " + device.getIpAddress());
        } catch (final IOException e) {
            throw new ConnectionException(e);
        } catch (final EncrypterException e) {
            LOGGER.error("decryption of security keys failed for device: {}", device.getDeviceIdentification(), e);
            throw new TechnicalException(ComponentType.PROTOCOL_DLMS,
                    "decryption of security keys failed for device: " + device.getDeviceIdentification());
        }
    }

    @Override
    protected void setSecurity(final DlmsDevice device, final TcpConnectionBuilder tcpConnectionBuilder)
            throws OsgpException {

        final byte[] password;
        try {
            password = this.securityKeyService.getDlmsPassword(device.getDeviceIdentification());
        } catch (final EncrypterException e) {
            LOGGER.error("Error determining DLMS password setting up LLS1 connection", e);
            throw new FunctionalException(FunctionalExceptionType.INVALID_DLMS_KEY_ENCRYPTION,
                    ComponentType.PROTOCOL_DLMS);
        }
        if (password == null) {
            LOGGER.error("There is no password available for device " + device.getDeviceIdentification());
            throw new FunctionalException(FunctionalExceptionType.INVALID_DLMS_KEY_ENCRYPTION,
                    ComponentType.PROTOCOL_DLMS);
        }

        final SecuritySuite securitySuite = SecuritySuite.builder()
                .setAuthenticationMechanism(AuthenticationMechanism.LOW).setPassword(password).build();

        tcpConnectionBuilder.setSecuritySuite(securitySuite).setClientId(this.clientAccessPoint);
    }

}
