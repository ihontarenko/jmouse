package org.jmouse.web.method;

import org.jmouse.beans.InitializingBean;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.mvc.MVCResult;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.context.WebBeanContext;

/**
 * 🔄 Base class for {@link ReturnValueHandler} implementations.
 *
 * <p>Provides lifecycle integration via {@link InitializingBean}
 * and delegates return value processing to subclasses.
 *
 * @author Ivan Hontarenko
 * @author ihontarenko@gmail.com
 */
public abstract class AbstractReturnValueHandler implements ReturnValueHandler, InitializingBeanSupport<WebBeanContext> {

    /**
     * 🔁 Delegates to {@link #doReturnValueHandle(MVCResult, RequestContext)}.
     */
    @Override
    public void handleReturnValue(MVCResult result, RequestContext requestContext) {
        doReturnValueHandle(result, requestContext);
    }

    /**
     * 🧪 Subclass-specific return value handling logic.
     *
     * @param result          actual return value + metadata
     * @param requestContext  request-specific state
     */
    protected abstract void doReturnValueHandle(MVCResult result, RequestContext requestContext);
}
