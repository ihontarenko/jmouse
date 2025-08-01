package org.jmouse.mvc.adapter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.mvc.MvcContainer;
import org.jmouse.mvc.ReturnValueHandler;

public class VoidReturnValueHandler implements ReturnValueHandler {

    @Override
    public boolean supportsReturnType(MvcContainer returnType) {
        return false;
    }

    @Override
    public void handleReturnValue(MvcContainer mvcContainer, HttpServletRequest request, HttpServletResponse response) {

    }
}
