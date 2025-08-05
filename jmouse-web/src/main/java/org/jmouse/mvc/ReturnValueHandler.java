package org.jmouse.mvc;

public interface ReturnValueHandler {

    boolean supportsReturnType(MethodParameter returnType, InvocationOutcome result);

    void handleReturnValue(InvocationOutcome result, RequestContext requestContext);

}
