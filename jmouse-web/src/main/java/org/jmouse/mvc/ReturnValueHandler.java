package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface ReturnValueHandler {

    boolean supportsReturnType(MvcContainer returnType);

    void handleReturnValue(MvcContainer mvcContainer, HttpServletRequest request, HttpServletResponse response);

}
