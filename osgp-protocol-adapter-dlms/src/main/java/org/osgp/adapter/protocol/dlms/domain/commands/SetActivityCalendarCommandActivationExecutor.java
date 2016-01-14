package org.osgp.adapter.protocol.dlms.domain.commands;

import java.io.IOException;
import java.util.List;

import org.openmuc.jdlms.LnClientConnection;
import org.openmuc.jdlms.MethodParameter;
import org.openmuc.jdlms.MethodResult;
import org.openmuc.jdlms.MethodResultCode;
import org.openmuc.jdlms.ObisCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component()
public class SetActivityCalendarCommandActivationExecutor implements CommandExecutor<Void, MethodResultCode> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetActivityCalendarCommandActivationExecutor.class);

    private static final int CLASS_ID = 20;
    private static final ObisCode OBIS_CODE = new ObisCode("0.0.13.0.0.255");
    private static final int METHOD_ID_ACTIVATE_PASSIVE_CALENDAR = 1;

    @Override
    public MethodResultCode execute(final LnClientConnection conn, final Void v) throws IOException {

        LOGGER.info("ACTIVATING PASSIVE CALENDAR");
        final MethodParameter method = new MethodParameter(CLASS_ID, OBIS_CODE, METHOD_ID_ACTIVATE_PASSIVE_CALENDAR);
        final List<MethodResult> methodResultCode = conn.action(method);
        if (methodResultCode == null || methodResultCode.isEmpty() || methodResultCode.get(0) == null) {
            throw new IOException("action method for ClientConnection should return a list with one MethodResult");
        }
        return methodResultCode.get(0).resultCode();
    }
}