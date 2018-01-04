/**
 * Copyright 2017 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.infra.messaging.responses.from.core.processors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgp.adapter.protocol.dlms.application.services.DomainHelperService;
import org.osgp.adapter.protocol.dlms.application.services.FirmwareService;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.domain.factories.DlmsConnectionFactory;
import org.osgp.adapter.protocol.dlms.domain.factories.DlmsConnectionHolder;
import org.osgp.adapter.protocol.dlms.exceptions.OsgpExceptionConverter;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;
import org.osgp.adapter.protocol.dlms.infra.messaging.DeviceResponseMessageSender;
import org.osgp.adapter.protocol.dlms.infra.messaging.DlmsLogItemRequestMessageSender;
import org.osgp.adapter.protocol.dlms.infra.messaging.DlmsMessageListener;
import org.osgp.adapter.protocol.dlms.infra.messaging.RetryHeaderFactory;
import org.osgp.adapter.protocol.dlms.infra.messaging.requests.to.core.OsgpRequestMessageType;
import org.osgp.adapter.protocol.jasper.sessionproviders.exceptions.SessionProviderException;

import com.alliander.osgp.dto.valueobjects.FirmwareFileDto;
import com.alliander.osgp.dto.valueobjects.smartmetering.UpdateFirmwareResponseDto;
import com.alliander.osgp.shared.exceptionhandling.OsgpException;
import com.alliander.osgp.shared.infra.jms.MessageMetadata;
import com.alliander.osgp.shared.infra.jms.ObjectMessageBuilder;
import com.alliander.osgp.shared.infra.jms.ResponseMessage;
import com.alliander.osgp.shared.infra.jms.ResponseMessageResultType;

public class GetFirmwareFileResponseMessageProcessorTest {

    @Mock
    protected DeviceResponseMessageSender responseMessageSender;

    @Mock
    protected DlmsLogItemRequestMessageSender dlmsLogItemRequestMessageSender;

    @Mock
    protected OsgpExceptionConverter osgpExceptionConverter;

    @Mock
    protected DomainHelperService domainHelperService;

    @Mock
    protected DlmsConnectionFactory dlmsConnectionFactory;

    @Mock
    protected DlmsMessageListener dlmsMessageListenerMock;

    @Mock
    private RetryHeaderFactory retryHeaderFactory;

    @Mock
    private FirmwareService firmwareService;

    @Mock
    private DlmsConnectionHolder dlmsConnectionHolderMock;

    @Mock
    private DlmsDevice dlmsDeviceMock;

    @InjectMocks
    private GetFirmwareFileResponseMessageProcessor getFirmwareFileResponseMessageProcessor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void processMessageShouldSendOkResponseMessageContainingFirmwareVersions()
            throws OsgpException, JMSException {
        // arrange
        final FirmwareFileDto firmwareFileDto = this.setupFirmwareFileDto();
        final ResponseMessage responseMessage = this.setupResponseMessage(firmwareFileDto);
        final ObjectMessage message = new ObjectMessageBuilder()
                .withMessageType(OsgpRequestMessageType.GET_FIRMWARE_FILE.name()).withObject(responseMessage).build();
        final UpdateFirmwareResponseDto updateFirmwareResponseDto = new UpdateFirmwareResponseDto(
                firmwareFileDto.getFirmwareIdentification(), new LinkedList<>());

        final ArgumentCaptor<ResponseMessage> responseMessageArgumentCaptor = ArgumentCaptor
                .forClass(ResponseMessage.class);

        when(this.domainHelperService.findDlmsDevice(any(MessageMetadata.class))).thenReturn(this.dlmsDeviceMock);
        when(this.dlmsConnectionFactory.getConnection(this.dlmsDeviceMock, null))
                .thenReturn(this.dlmsConnectionHolderMock);
        when(this.dlmsConnectionHolderMock.getDlmsMessageListener()).thenReturn(this.dlmsMessageListenerMock);
        when(this.dlmsDeviceMock.isInDebugMode()).thenReturn(false);
        when(this.firmwareService.updateFirmware(this.dlmsConnectionHolderMock, this.dlmsDeviceMock, firmwareFileDto))
                .thenReturn(updateFirmwareResponseDto);

        // act
        this.getFirmwareFileResponseMessageProcessor.processMessage(message);

        // assert
        verify(this.responseMessageSender, times(1)).send(responseMessageArgumentCaptor.capture());

        assertThat(responseMessageArgumentCaptor.getValue().getDataObject(), is(updateFirmwareResponseDto));
        assertThat(responseMessageArgumentCaptor.getValue().getResult(), is(ResponseMessageResultType.OK));
    }

    // @Test
    public void handleMessageShouldCallUpdateFirmware()
            throws OsgpException, ProtocolAdapterException, SessionProviderException {
        // arrange
        final FirmwareFileDto firmwareFileDto = this.setupFirmwareFileDto();
        final ResponseMessage responseMessage = this.setupResponseMessage(firmwareFileDto);

        // act
        this.getFirmwareFileResponseMessageProcessor.handleMessage(this.dlmsConnectionHolderMock, this.dlmsDeviceMock,
                responseMessage);

        // assert
        verify(this.firmwareService, times(1)).updateFirmware(this.dlmsConnectionHolderMock, this.dlmsDeviceMock,
                firmwareFileDto);
    }

    private FirmwareFileDto setupFirmwareFileDto() {
        return new FirmwareFileDto("fw", "fw".getBytes());
    }

    private ResponseMessage setupResponseMessage(final FirmwareFileDto firmwareFileDto) {
        return new ResponseMessage("corr-uid-1", "test-org", "dvc-01", ResponseMessageResultType.OK, null,
                firmwareFileDto);
    }

}
