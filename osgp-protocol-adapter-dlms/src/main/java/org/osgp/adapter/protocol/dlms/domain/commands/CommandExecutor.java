/**
 * Copyright 2015 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.dlms.domain.commands;

import org.openmuc.jdlms.LnClientConnection;
import org.osgp.adapter.protocol.dlms.domain.entities.DlmsDevice;
import org.osgp.adapter.protocol.dlms.exceptions.ProtocolAdapterException;

/**
 * Interface for executing a command on a smart meter over a client connection,
 * taking input of type <T>.
 *
 * @param <T>
 *            the type of object used as input for executing a command.
 * @param <R>
 *            the type of object returned as a result from executing a command.
 */
public interface CommandExecutor<T, R> {

    R execute(LnClientConnection conn, DlmsDevice device, T object) throws ProtocolAdapterException;

}
