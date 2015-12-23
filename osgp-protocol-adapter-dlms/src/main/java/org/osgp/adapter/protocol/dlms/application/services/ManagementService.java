/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.application.services;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.osgp.adapter.protocol.dlms.domain.repositories.DlmsDeviceRepository;
import org.osgp.adapter.protocol.dlms.infra.messaging.DeviceResponseMessageSender;
import org.osgp.adapter.protocol.dlms.infra.messaging.DlmsDeviceMessageMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alliander.osgp.dto.valueobjects.smartmetering.Event;
import com.alliander.osgp.dto.valueobjects.smartmetering.EventMessageDataContainer;
import com.alliander.osgp.dto.valueobjects.smartmetering.FindEventsQuery;
import com.alliander.osgp.dto.valueobjects.smartmetering.FindEventsQueryMessageDataContainer;
import com.alliander.osgp.shared.exceptionhandling.ComponentType;
import com.alliander.osgp.shared.exceptionhandling.TechnicalException;
import com.alliander.osgp.shared.infra.jms.ResponseMessageResultType;

@Service(value = "dlmsManagementService")
public class ManagementService extends DlmsApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementService.class);

    @Autowired
    private DlmsDeviceRepository dlmsDeviceRepository;

    // === FIND EVENTS ===

    public void findEvents(final DlmsDeviceMessageMetadata messageMetadata,
            final DeviceResponseMessageSender responseMessageSender,
            final FindEventsQueryMessageDataContainer findEventsQueryMessageDataContainer) {

        this.logStart(LOGGER, messageMetadata, "findEvents");

        try {
            // Debug logging which can be removed.
            LOGGER.info("FindEventsQueryMessageDataContainer number of FindEventsQuery: {}",
                    findEventsQueryMessageDataContainer.getFindEventsQueryList().size());
            for (final FindEventsQuery findEventsQuery : findEventsQueryMessageDataContainer.getFindEventsQueryList()) {
                LOGGER.info(
                        "findEventsQuery.eventLogCategory :{}, findEventsQuery.from: {}, findEventsQuery.until: {}",
                        findEventsQuery.getEventLogCategory().toString(), findEventsQuery.getFrom(),
                        findEventsQuery.getUntil());
            }

            // TODO: talk to the smart-meter and fetch the events.
            // For now, just create some dummy data to return.

            final List<Event> events = new ArrayList<>();
            events.add(new Event(DateTime.now(), 1));
            events.add(new Event(DateTime.now(), 2));
            events.add(new Event(DateTime.now(), 3));
            events.add(new Event(DateTime.now(), 4));
            events.add(new Event(DateTime.now(), 5));
            events.add(new Event(DateTime.now(), 6));
            events.add(new Event(DateTime.now(), 7));
            events.add(new Event(DateTime.now(), 8));
            events.add(new Event(DateTime.now(), 9));
            events.add(new Event(DateTime.now(), 10));
            events.add(new Event(DateTime.now(), 11));
            events.add(new Event(DateTime.now(), 12));
            final EventMessageDataContainer eventMessageDataContainer = new EventMessageDataContainer(events);

            this.sendResponseMessage(messageMetadata, ResponseMessageResultType.OK, null, responseMessageSender,
                    eventMessageDataContainer);

        } catch (final Exception e) {
            LOGGER.error("Unexpected exception during findEvents", e);
            final TechnicalException ex = new TechnicalException(ComponentType.UNKNOWN,
                    "Unexpected exception while retrieving response message", e);

            this.sendResponseMessage(messageMetadata, ResponseMessageResultType.NOT_OK, ex, responseMessageSender, null);
        }
    }

}
