package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 🔌 Delegates return value processing to the appropriate ReturnValueHandler.
 */
public class ReturnValueProcessor {

    private final List<ReturnValueHandler> handlers;

    public ReturnValueProcessor(List<ReturnValueHandler> handlers) {
        this.handlers = handlers;
    }

    public void process(MvcContainer container, HttpServletRequest request, HttpServletResponse response)  {
        for (ReturnValueHandler handler : handlers) {
            if (handler.supportsReturnType(container)) {
                handler.handleReturnValue(container, request, response);
                return;
            }
        }
    }
}
