package org.jmouse.mvc;

public interface ReturnValueHandler {

    boolean supportsReturnType(Class<?> returnType);

    void handleReturnValue(HandlerResult handlerResult);

}
