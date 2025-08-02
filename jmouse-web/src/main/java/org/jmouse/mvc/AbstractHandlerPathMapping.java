package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.InitializingBean;
import org.jmouse.mvc.routing.MappingMatcher;
import org.jmouse.mvc.routing.RouteMapping;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.RequestRoute;
import org.jmouse.web.request.http.HttpMethod;

import java.util.*;

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
 *   <li>Typed routing via {@link PathPattern} and {@link RouteMatch}</li>
 *   <li>Interceptor support via {@link HandlerInterceptorRegistry}</li>
 * </ul>
 *
 * @param <H> handler type (e.g. Controller, Function, etc.)
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public abstract class AbstractHandlerPathMapping<H> extends AbstractHandlerMapping implements InitializingBean {

    /**
     * 🗺️ Map of path patterns to handlers
     */
    private final Map<Route, H>              handlers = new HashMap<>();
    private       HandlerInterceptorRegistry registry;

    /**
     * ➕ Registers a route and its corresponding handler.
     *
     * @param route   path pattern, e.g. {@code /user/{id}}
     * @param handler handler instance
     */
    public void addHandlerMapping(Route route, H handler) {
        handlers.put(route, handler);
    }

    @Override
    public void afterCompletion(BeanContext context) {
        initialize((WebBeanContext) context);
    }

    /**
     * 🔍 Resolves the handler and route info for a given request.
     *
     * @param request HTTP request
     * @return matched handler with route info, or null if no match
     */
    public MappedHandler getMappedHandler(HttpServletRequest request) {
        String        mappingPath   = getMappingPath(request);
        HttpMethod    httpMethod    = HttpMethod.valueOf(request.getMethod());
        MappedHandler mappedHandler = null;
        RequestRoute requestRoute = RequestRoute.ofRequest(request);

        List<RouteMapping> mappings  = new ArrayList<>();

        for (Route route : handlers.keySet()) {
            mappings.add(new RouteMapping(route));
        }

        List<RouteMapping> candidates = new ArrayList<>();

        for (RouteMapping mapping : mappings) {
            if (mapping.matches(requestRoute)) {
                candidates.add(mapping);
            }
        }

        candidates.sort((a, b) -> -1 * a.compareWith(b, requestRoute));

        RouteMapping winner = candidates.getFirst();

        System.out.println(candidates);

        for (Map.Entry<Route, H> entry : handlers.entrySet()) {
            Route       route       = entry.getKey();
            PathPattern pathPattern = route.pathPattern();
            HttpMethod  method      = route.httpMethod();

            if (pathPattern.matches(mappingPath) && method.equals(httpMethod)) {
                RouteMatch routeMatch = pathPattern.parse(mappingPath);

                mappedHandler = new RouteMappedHandler(entry.getValue(), routeMatch, route);

                request.setAttribute(ROUTE_MACTH_ATTRIBUTE, routeMatch);
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
