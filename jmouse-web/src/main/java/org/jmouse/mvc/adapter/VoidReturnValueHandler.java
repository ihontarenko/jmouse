package org.jmouse.mvc.adapter;

import org.jmouse.mvc.ExecutionResult;
import org.jmouse.mvc.ReturnValueHandler;

public class VoidReturnValueHandler implements ReturnValueHandler {

    @Override
    public boolean supportsReturnType(Object returnValue) {
        return returnValue == null;
    }

    @Override
    public void handleReturnValue(ExecutionResult executionResult) {
        // nothing to write
    }
}
