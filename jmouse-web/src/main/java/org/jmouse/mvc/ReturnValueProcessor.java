package org.jmouse.mvc;

import java.util.List;

/**
 * 🔌 Delegates return value processing to the appropriate ReturnValueHandler.
 */
public class ReturnValueProcessor {

    private final List<ReturnValueHandler> handlers;

    public ReturnValueProcessor(List<ReturnValueHandler> handlers) {
        this.handlers = handlers;
    }

    public void process(MethodParameter returnType, InvocationOutcome container, RequestContext requestContext)  {
        for (ReturnValueHandler handler : handlers) {
            if (handler.supportsReturnType(returnType, container)) {
                handler.handleReturnValue(container, requestContext);
            }
        }
    }
}
