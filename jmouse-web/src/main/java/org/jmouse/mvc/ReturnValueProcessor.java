package org.jmouse.mvc;

import java.util.List;

/**
 * 🔌 Delegates return value processing to the appropriate {@link ReturnValueHandler}.
 *
 * <p>Used by the framework to post-process the result returned from a controller method.
 * Scans the available handlers and invokes the one that supports the given return type.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ReturnValueProcessor {

    private final List<ReturnValueHandler> handlers;

    /**
     * 🔧 Constructs the processor with the available return value handlers.
     *
     * @param handlers list of {@link ReturnValueHandler} to delegate processing to
     */
    public ReturnValueProcessor(List<ReturnValueHandler> handlers) {
        this.handlers = handlers;
    }

    /**
     * 🚚 Processes the return value using a matching {@link ReturnValueHandler}.
     *
     * @param returnType the declared return type of the method
     * @param outcome the invocation result to be processed
     * @param requestContext current request/response context
     */
    public void process(MethodParameter returnType, InvocationOutcome outcome, RequestContext requestContext) {
        for (ReturnValueHandler handler : handlers) {
            if (handler.supportsReturnType(returnType, outcome)) {
                handler.handleReturnValue(outcome, requestContext);
            }
        }
    }
}
