package org.jmouse.mvc.adapter.return_value;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.mvc.InvocationResult;
import org.jmouse.mvc.MethodParameter;
import org.jmouse.mvc.adapter.AbstractReturnValueHandler;
import org.jmouse.web.context.WebBeanContext;

public class ViewReturnValueHandler extends AbstractReturnValueHandler {

    @Override
    protected void doInitialize(WebBeanContext context) {

    }

    @Override
    protected void doReturnValueHandle(InvocationResult result, HttpServletRequest request, HttpServletResponse response) {

    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType, InvocationResult result) {
        System.out.println(result);
        return false;
    }

}
