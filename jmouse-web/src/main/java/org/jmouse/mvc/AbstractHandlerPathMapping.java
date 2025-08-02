package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.InitializingBean;
import org.jmouse.mvc.routing.MappingRegistration;
import org.jmouse.mvc.routing.MappingRegistry;
import org.jmouse.mvc.routing.RouteMapping;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.RequestRoute;

import java.util.ArrayList;
import java.util.List;

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

    private final MappingRegistry<H, RouteMapping> mappingRegistry = new MappingRegistry<>();
    /**
     * 🗺️ Map of path patterns to handlers
     */
    private       HandlerInterceptorRegistry       interceptorRegistry;

    /**
     * ➕ Registers a route and its corresponding handler.
     *
     * @param route   path pattern, e.g. {@code /user/{id}}
     * @param handler handler instance
     */
    public void addHandlerMapping(Route route, H handler) {
        RouteMapping mapping = new RouteMapping(route);
        mappingRegistry.register(mapping, new MappingRegistration<>(mapping, handler));
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
        RequestRoute  requestRoute  = RequestRoute.ofRequest(request);
        MappedHandler mappedHandler = null;

        RouteMapping winner = matchRouteMapping(requestRoute);

        if (winner != null) {
            MappingRegistration<H, RouteMapping> registration = mappingRegistry.getRegistration(winner);
            H                                    handler      = registration.handler();
            Route                                route        = winner.getRoute();

            RouteMatch routeMatch = route.pathPattern().parse(requestRoute.requestPath().requestPath());

            mappedHandler = new RouteMappedHandler(handler, routeMatch, route);

            request.setAttribute(ROUTE_MACTH_ATTRIBUTE, routeMatch);
        }

        return mappedHandler;
    }

    private RouteMapping matchRouteMapping(RequestRoute requestRoute) {
        List<RouteMapping> candidates = new ArrayList<>();

        for (RouteMapping mapping : mappingRegistry.getMappings()) {
            if (mapping.matches(requestRoute)) {
                candidates.add(mapping);
            }
        }

        candidates.sort((a, b)
                -> -1 * a.compare(b, requestRoute));

        return candidates.getFirst();
    }

    @Override
    protected List<HandlerInterceptor> getHandlerInterceptors() {
        return interceptorRegistry.getInterceptors();
    }

    /**
     * 🛠️ Sets the interceptor registry (usually injected during init).
     *
     * @param registry configured registry
     */
    public void setHandlerInterceptorsRegistry(HandlerInterceptorRegistry registry) {
        this.interceptorRegistry = registry;
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
