package org.jmouse.web.mvc.method;

import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.web.mvc.MVCResult;
import org.jmouse.core.Sorter;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.RequestContext;

import java.util.ArrayList;
import java.util.Collections;
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
public class ReturnValueProcessor implements InitializingBeanSupport<WebBeanContext> {

    private List<ReturnValueHandler> handlers;

    /**
     * 🔧 Constructs the processor with empty handlers list.
     */
    public ReturnValueProcessor() {
        this(Collections.emptyList());
    }

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
     */
    public void process(MVCResult result, RequestContext requestContext) {
        for (ReturnValueHandler handler : handlers) {
            if (handler.supportsReturnType(result)) {
                handler.handleReturnValue(result, requestContext);
                return;
            }
        }
    }

    @Override
    public void initialize(WebBeanContext context) {
        List<ReturnValueHandler> returnValueHandlers = new ArrayList<>(context.getBeans(ReturnValueHandler.class));
        Sorter.sort(returnValueHandlers);
        handlers = List.copyOf(returnValueHandlers);
    }
}
