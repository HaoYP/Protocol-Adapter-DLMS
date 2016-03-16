/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.commands;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.openmuc.jdlms.AttributeAddress;
import org.openmuc.jdlms.GetResult;
import org.openmuc.jdlms.LnClientConnection;
import org.openmuc.jdlms.ObisCode;
import org.openmuc.jdlms.datatypes.DataObject;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.exceptions.ConnectionException;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GetFirmwareVersionCommandExecutor implements CommandExecutor<Void, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetFirmwareVersionCommandExecutor.class);

    private static final int CLASS_ID = 1;
    private static final ObisCode OBIS_CODE = new ObisCode("1.0.0.2.0.255");
    private static final int ATTRIBUTE_ID = 2;

    @Autowired
    private DlmsHelperService dlmsHelperService;

    @Override
    public String execute(final LnClientConnection conn, final DlmsDevice device, final Void useless)
            throws ProtocolAdapterException {

        LOGGER.info(
                "Retrieving firmware version by issuing get request for class id: {}, obis code: {}, attribute id: {}",
                CLASS_ID, OBIS_CODE, ATTRIBUTE_ID);

        final AttributeAddress firmwareVersionValue = new AttributeAddress(CLASS_ID, OBIS_CODE, ATTRIBUTE_ID);

        List<GetResult> getResultList;
        try {
            getResultList = conn.get(firmwareVersionValue);
        } catch (IOException | TimeoutException e) {
            throw new ConnectionException(e);
        }

        if (getResultList.isEmpty()) {
            throw new ProtocolAdapterException("No GetResult received while retrieving firmware version.");
        }

        if (getResultList.size() > 1) {
            throw new ProtocolAdapterException("Expected 1 GetResult while retrieving firmware version, got "
                    + getResultList.size());
        }

        final GetResult result = getResultList.get(0);
        final DataObject resultData = result.resultData();

        if (!resultData.isByteArray()) {
            throw new ProtocolAdapterException("Unexpected value returned by meter while retrieving firmware version.");
        }

        return new String((byte[]) resultData.value(), StandardCharsets.US_ASCII);
    }
}
