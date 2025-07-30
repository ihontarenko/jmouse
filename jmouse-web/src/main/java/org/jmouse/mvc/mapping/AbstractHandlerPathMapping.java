package org.jmouse.mvc.mapping;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanContextAware;
import org.jmouse.mvc.*;
import org.jmouse.web.context.WebBeanContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🧭 Abstract base for route-based handler mappings.
 * Matches incoming HTTP requests by {@link PathPattern} and resolves handlers of type {@code H}.
 *
 * <p>💡 Example:
 * <pre>{@code
 * AbstractHandlerPathMapping<Controller> mapping = ...;
 * mapping.addHandlerMapping("/user/{id}", new UserController());
 * }</pre>
 * <p>
 * Supports:
 * <ul>
 *   <li>Typed routing via {@link PathPattern} and {@link RoutePath}</li>
 *   <li>DI via {@link BeanContextAware}</li>
 *   <li>Interceptor support via {@link HandlerInterceptorRegistry}</li>
 * </ul>
 *
 * @param <H> handler type (e.g. Controller, Function, etc.)
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public abstract class AbstractHandlerPathMapping<H> extends AbstractHandlerMapping implements BeanContextAware {

    /**
     * 🗺️ Map of path patterns to handlers
     */
    private final Map<PathPattern, H> handlers = new HashMap<>();

    private HandlerInterceptorRegistry registry;
    private WebBeanContext             context;

    /**
     * ➕ Registers a route and its corresponding handler.
     *
     * @param route   path pattern, e.g. {@code /user/{id}}
     * @param handler handler instance
     */
    public void addHandlerMapping(String route, H handler) {
        handlers.put(new PathPattern(route), handler);
    }

    @Override
    public BeanContext getBeanContext() {
        return context;
    }

    @Override
    public void setBeanContext(BeanContext context) {
        this.context = (WebBeanContext) context;
        initialize(this.context);
    }

    /**
     * 🔍 Resolves the handler and route info for a given request.
     *
     * @param request HTTP request
     * @return matched handler with route info, or null if no match
     */
    public MappedHandler getMappedHandler(HttpServletRequest request) {
        MappedHandler mappedHandler = null;
        String        mappingPath   = getMappingPath(request);

        for (Map.Entry<PathPattern, H> entry : handlers.entrySet()) {
            PathPattern pathPattern = entry.getKey();

            if (pathPattern.matches(mappingPath)) {
                RoutePath routePath = pathPattern.parse(mappingPath);
                mappedHandler = new MappedHandler(entry.getValue(), routePath);
                request.setAttribute(ROUTE_PATH_ATTRIBUTE, routePath);
                break;
            }
        }

        return mappedHandler;
    }

    @Override
    protected List<HandlerInterceptor> getHandlerInterceptors() {
        return registry.getInterceptors();
    }

    /**
     * 🛠️ Sets the interceptor registry (usually injected during init).
     *
     * @param registry configured registry
     */
    public void setHandlerInterceptorsRegistry(HandlerInterceptorRegistry registry) {
        this.registry = registry;
    }

    /**
     * ⚙️ Initializes the mapping with context and interceptors.
     *
     * @param context current web bean context
     */
    protected void initialize(WebBeanContext context) {
        setHandlerInterceptorsRegistry(context.getBean(HandlerInterceptorRegistry.class));
        doInitialize(context);
    }

    /**
     * 🔧 Hook for subclasses to register handlers.
     *
     * @param context current context
     */
    protected abstract void doInitialize(WebBeanContext context);
}
