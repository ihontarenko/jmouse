package org.jmouse.mvc.adapter;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.InitializingBean;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.mvc.InvocationOutcome;
import org.jmouse.mvc.MethodParameter;
import org.jmouse.mvc.RequestContext;
import org.jmouse.mvc.ReturnValueHandler;
import org.jmouse.web.context.WebBeanContext;

/**
 * 🔄 Base class for {@link ReturnValueHandler} implementations.
 *
 * <p>Provides lifecycle integration via {@link InitializingBean}
 * and delegates return value processing to subclasses.
 *
 * <p>Typical usage:
 * <pre>{@code
 * public class JsonReturnValueHandler extends AbstractReturnValueHandler {
 *     protected void doReturnValueHandle(MethodParameter returnType, InvocationOutcome result, RequestContext ctx) {
 *         // Serialize result to JSON and write to response
 *     }
 *
 *     protected void doInitialize(WebBeanContext context) {
 *         // Load ObjectMapper, etc.
 *     }
 * }
 * }</pre>
 *
 * @author Ivan Hontarenko
 * @author ihontarenko@gmail.com
 */
public abstract class AbstractReturnValueHandler implements ReturnValueHandler, InitializingBeanSupport<WebBeanContext> {

    /**
     * 🔁 Delegates to {@link #doReturnValueHandle(InvocationOutcome, RequestContext)}.
     */
    @Override
    public void handleReturnValue(InvocationOutcome outcome, RequestContext requestContext) {
        doReturnValueHandle(outcome, requestContext);
    }

    /**
     * 🧪 Subclass-specific return value handling logic.
     *
     * @param result          actual return value + metadata
     * @param requestContext  request-specific state
     */
    protected abstract void doReturnValueHandle(InvocationOutcome result, RequestContext requestContext);
}
