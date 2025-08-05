package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface ReturnValueHandler {

    boolean supportsReturnType(MethodParameter returnType, InvocationResult result);

    void handleReturnValue(InvocationResult result, HttpServletRequest request, HttpServletResponse response);

}
