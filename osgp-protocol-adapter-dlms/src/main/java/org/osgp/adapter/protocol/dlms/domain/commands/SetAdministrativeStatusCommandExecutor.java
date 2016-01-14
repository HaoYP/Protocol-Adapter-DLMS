/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.commands;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.openmuc.jdlms.AccessResultCode;
import org.openmuc.jdlms.AttributeAddress;
import org.openmuc.jdlms.LnClientConnection;
import org.openmuc.jdlms.ObisCode;
import org.openmuc.jdlms.SetParameter;
import org.openmuc.jdlms.datatypes.DataObject;
import org.osgp.adapter.protocol.dlms.application.mapping.ConfigurationMapper;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alliander.osgp.dto.valueobjects.smartmetering.AdministrativeStatusType;

@Component()
public class SetAdministrativeStatusCommandExecutor implements
        CommandExecutor<AdministrativeStatusType, AccessResultCode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetAdministrativeStatusCommandExecutor.class);

    private static final int CLASS_ID = 1;
    private static final ObisCode OBIS_CODE = new ObisCode("0.1.94.31.0.255");
    private static final int ATTRIBUTE_ID = 2;

    @Autowired
    private ConfigurationMapper configurationMapper;

    @Override
    public AccessResultCode execute(final LnClientConnection conn,
            final AdministrativeStatusType administrativeStatusType) throws IOException, TimeoutException,
            ProtocolAdapterException {

        LOGGER.info(
                "Set administrative status by issuing get request for class id: {}, obis code: {}, attribute id: {}",
                CLASS_ID, OBIS_CODE, ATTRIBUTE_ID);

        final AttributeAddress attributeAddress = new AttributeAddress(CLASS_ID, OBIS_CODE, ATTRIBUTE_ID);
        final DataObject value = DataObject.newEnumerateData(this.configurationMapper.map(administrativeStatusType,
                Integer.class));

        final SetParameter setParameter = new SetParameter(attributeAddress, value);

        return conn.set(setParameter).get(0);
    }
}