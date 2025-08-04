package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface ReturnValueHandler {

    boolean supportsReturnType(InvocationResult returnType);

    void handleReturnValue(InvocationResult mvcContainer, HttpServletRequest request, HttpServletResponse response);

}
