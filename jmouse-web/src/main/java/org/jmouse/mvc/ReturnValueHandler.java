package org.jmouse.mvc;

public interface ReturnValueHandler {

    boolean supportsReturnType(Object returnType);

    void handleReturnValue(ExecutionResult executionResult);

}
