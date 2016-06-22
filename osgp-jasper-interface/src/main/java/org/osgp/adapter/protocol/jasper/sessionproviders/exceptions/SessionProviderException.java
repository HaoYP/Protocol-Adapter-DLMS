/**
 * Copyright 2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.osgp.adapter.protocol.jasper.sessionproviders.exceptions;

public class SessionProviderException extends Exception {

    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = -5449632007365870329L;

    public SessionProviderException(final String message) {
        super(message);
    }

    public SessionProviderException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
}