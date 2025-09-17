package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.InitializingBean;
import org.jmouse.core.Sorter;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.mvc.cors.CorsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 🧭 Base class for handler mappings.
 *
 * Provides common logic for resolving a handler and attaching sorted interceptors.
 * Subclasses must implement handler lookup and interceptor list retrieval.
 *
 * Example usage:
 * <pre>{@code
 * public class MyHandlerMapping extends AbstractHandlerMapping {
 *     protected Object doGetHandler(HttpServletRequest req) { ... }
 *     protected List<HandlerInterceptor> getHandlerInterceptors() { ... }
 * }
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public abstract class AbstractHandlerMapping implements HandlerMapping, InitializingBean {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractHandlerMapping.class);

    /**
     * 🔎 Resolves the handler for the current request and attaches interceptors.
     *
     * @param request current HTTP request
     * @return container with handler and interceptors, or {@code null} if no handler found
     */
    @Override
    public Handler getHandler(HttpServletRequest request) {
        MappedHandler handler = doGetHandler(request);

        if (handler == null) {
            return null;
        }

        Handler                  container    = new Handler(handler);
        List<HandlerInterceptor> interceptors = new ArrayList<>(getHandlerInterceptors());

        if (!interceptors.isEmpty()) {
            Sorter.sort(interceptors);
            interceptors.forEach(container::addInterceptor);
        }

        return container;
    }

    /**
     * 🔄 Called after DI context is fully initialized.
     *
     * @param context fully initialized context (should be WebBeanContext)
     */
    @Override
    public void afterCompletion(BeanContext context) {
        initialize((WebBeanContext) context);
    }

    /**
     * ⚙️ Initializes the mapping
     *
     * @param context current web bean context
     */
    protected void initialize(WebBeanContext context) {
        doInitialize(context);
    }

    /**
     * 🎯 Resolve handler object from request.
     *
     * @param request HTTP request
     * @return handler object or {@code null} if not matched
     */
    protected abstract MappedHandler doGetHandler(HttpServletRequest request);

    /**
     * 🧩 List of interceptors for current mapping.
     *
     * @return list of interceptors (can be empty)
     */
    protected abstract List<HandlerInterceptor> getHandlerInterceptors();

    /**
     * 🔧 Hook for subclasses to register handlers.
     *
     * @param context current context
     */
    protected abstract void doInitialize(WebBeanContext context);
}
