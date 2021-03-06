/**
 * Copyright 2016 Smart Society Services B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.osgp.adapter.protocol.jasper.infra.ws;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.stereotype.Service;

@Service
public class CorrelationIdProviderService {

    public String getCorrelationId(final String type, final String iccid) {

        return type + "|||" + iccid + "|||" + this.getCurrentDateString();
    }

    private String getCurrentDateString() {
        final Date now = new Date();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddkkmmssSSS");
        return sdf.format(now);
    }
}
