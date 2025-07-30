package org.jmouse.mvc.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.mvc.HandlerResult;
import org.jmouse.mvc.ReturnValueHandler;

public class VoidReturnValueHandler implements ReturnValueHandler {

    @Override
    public boolean supportsReturnType(Object returnValue) {
        return returnValue == null;
    }

    @Override
    public void handleReturnValue(HandlerResult handlerResult) {
        // nothing to write
    }
}
