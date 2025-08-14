package org.jmouse.web;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.InitializingBean;
import org.jmouse.mvc.MVCResult;
import org.jmouse.mvc.MappedHandler;
import org.jmouse.web.http.request.RequestContext;
import org.jmouse.web.context.WebBeanContext;

/**
 * ⚠️ Base class for {@link ExceptionResolver} implementations.
 *
 * Provides lifecycle support and defines the core template methods
 * for exception resolution and initialization within the web context.
 *
 * <p>Implements {@link InitializingBean} to hook into framework initialization.</p>
 *
 * <pre>{@code
 * public class ValidationExceptionResolver extends AbstractExceptionResolver {
 *     @Override
 *     protected void doInitialize(WebBeanContext context) {
 *         // Load templates, set up message sources, etc.
 *     }
 *
 *     @Override
 *     protected void doExceptionResolve(RequestContext ctx, MappedHandler handler, Exception ex) {
 *         if (ex instanceof ValidationException ve) {
 *             ctx.response().setStatus(400);
 *             ctx.response().render("validation-error", Map.of("errors", ve.getErrors()));
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author Ivan Hontarenko
 * @author ihontarenko@gmail.com
 */
public abstract class AbstractExceptionResolver
        implements ExceptionResolver, InitializingBean {

    /**
     * 📛 Entry point called by the dispatcher on exception.
     * Delegates to {@link #doExceptionResolve(RequestContext, MappedHandler, Exception)}.
     *
     * @param requestContext current request context
     * @param mappedHandler  the handler that was executing
     * @param exception      the exception thrown
     */
    @Override
    public MVCResult resolveException(RequestContext requestContext, MappedHandler mappedHandler, Exception exception) {
        return doExceptionResolve(requestContext, mappedHandler, exception);
    }

    /**
     * 🧩 Called after bean context is initialized.
     * Casts context to {@link WebBeanContext} and calls {@link #doInitialize(WebBeanContext)}.
     *
     * @param context the initialized bean context
     */
    @Override
    public void afterCompletion(BeanContext context) {
        initialize((WebBeanContext) context);
    }

    /**
     * 🛠️ Hook method for context-specific initialization.
     *
     * @param context web bean context
     */
    protected void initialize(WebBeanContext context) {
        doInitialize(context);
    }

    /**
     * 🔧 Subclasses implement this to perform initialization logic.
     *
     * @param context web bean context
     */
    protected abstract void doInitialize(WebBeanContext context);

    /**
     * 💥 Template method for handling exceptions.
     *
     * @param requestContext current request context
     * @param mappedHandler  the matched handler
     * @param exception      the thrown exception
     */
    protected abstract MVCResult doExceptionResolve(
            RequestContext requestContext, MappedHandler mappedHandler, Exception exception);
}
