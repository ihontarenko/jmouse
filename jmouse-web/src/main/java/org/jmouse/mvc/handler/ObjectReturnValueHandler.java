package org.jmouse.mvc.handler;

import org.jmouse.mvc.HandlerResult;
import org.jmouse.mvc.ReturnValueHandler;

public class ObjectReturnValueHandler implements ReturnValueHandler {

    @Override
    public boolean supportsReturnType(Class<?> returnType) {
        return false;
    }

    @Override
    public void handleReturnValue(HandlerResult handlerResult) {

    }

}
