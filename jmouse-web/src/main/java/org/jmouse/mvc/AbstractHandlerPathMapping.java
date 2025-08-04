package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.mvc.routing.MappingRegistration;
import org.jmouse.mvc.routing.MappingRegistry;
import org.jmouse.mvc.routing.MappingCriteria;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.RequestRoute;

import java.util.ArrayList;
import java.util.List;

/**
 * 🧭 Abstract base class for route-based handler mappings.
 * <p>
 * Matches incoming HTTP requests against {@link PathPattern} routes and provides
 * the matching handler of type {@code H}.
 *
 * <p>Usage example:
 * <pre>{@code
 * AbstractHandlerPathMapping<Controller> mapping = ...;
 * mapping.addHandlerMapping(Route.GET("/user/{id}"), new UserController());
 * }</pre>
 *
 * <p>Supports:
 * <ul>
 *   <li>Typed route matching via {@link PathPattern} and {@link RouteMatch}</li>
 *   <li>Dynamic handler registration via {@link #addHandlerMapping(Route, Object)}</li>
 *   <li>Request-scoped route resolution with {@link #getMappedHandler(HttpServletRequest)}</li>
 *   <li>Interceptor support via {@link HandlerInterceptorRegistry}</li>
 * </ul>
 *
 * <p>Subclasses should implement {@link #doInitialize(WebBeanContext)} to register their handlers
 * during context initialization.
 *
 * @param <H> the handler type (e.g. controller, function, command handler, etc.)
 * @author Ivan Hontarenko
 * @see MappingRegistry
 * @see MappingCriteria
 * @see RouteMappedHandler
 * @see WebBeanContext
 */
public abstract class AbstractHandlerPathMapping<H> extends AbstractHandlerMapping {

    private MappingRegistry<H>         mappingRegistry;
    private HandlerInterceptorRegistry interceptorRegistry;

    /**
     * ➕ Registers a route and its corresponding handler.
     *
     * @param route   path pattern, e.g. {@code /user/{id}}
     * @param handler handler instance
     */
    public void addHandlerMapping(Route route, H handler) {
        MappingRegistration<H> registration = new MappingRegistration<>(new MappingCriteria(route), handler);
        mappingRegistry.register(registration.criteria(), registration);
    }

    /**
     * 📍 Resolves the {@link MappedHandler} for the incoming request,
     * including route match and handler metadata.
     *
     * Example:
     * <pre>{@code
     * MappedHandler handler = mappingHandlerAdapter.getMappedHandler(request);
     * }</pre>
     *
     * @param request HTTP servlet request
     * @return {@link MappedHandler} with parsed route and handler, or {@code null} if no match found
     */
    public MappedHandler getMappedHandler(HttpServletRequest request) {
        RequestRoute    requestRoute  = RequestRoute.ofRequest(request);
        MappedHandler   mappedHandler = null;
        MappingCriteria winner        = getWinner(requestRoute);

        if (winner != null) {
            MappingRegistration<H> registration = mappingRegistry.getRegistration(winner);

            H          handler = registration.handler();
            Route      route   = winner.getRoute();
            RouteMatch match   = route.pathPattern().parse(requestRoute.requestPath().path());

            mappedHandler = new RouteMappedHandler(handler, match, route);

            request.setAttribute(ROUTE_MACTH_ATTRIBUTE, match);
        }

        return mappedHandler;
    }

    /**
     * 🥇 Selects the most specific and applicable {@link MappingCriteria}
     * that matches the incoming {@link RequestRoute}.
     *
     * This method:
     * <ul>
     *     <li>Filters all matching criteria</li>
     *     <li>Sorts them by specificity (using {@code compare()})</li>
     *     <li>Returns the most specific match</li>
     * </ul>
     *
     * @param requestRoute parsed request route from incoming HTTP request
     * @return best matching {@link MappingCriteria}, or {@code null} if none matched
     */
    private MappingCriteria getWinner(RequestRoute requestRoute) {
        List<MappingCriteria> candidates = new ArrayList<>();

        for (MappingCriteria mapping : mappingRegistry.getMappingCriteria()) {
            if (mapping.matches(requestRoute)) {
                candidates.add(mapping);
            }
        }

        candidates.sort((a, b)
                -> -1 * a.compare(b, requestRoute));

        return candidates.isEmpty() ? null : candidates.getFirst();
    }

    /**
     * 🧱 Returns all registered {@link HandlerInterceptor}s to be applied
     * to the current request processing pipeline.
     *
     * @return list of interceptors
     */
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
     * 📦 Returns the current {@link MappingRegistry} instance used to store {@link MappingCriteria}s.
     *
     * @return the configured mapping registry
     */
    public MappingRegistry<H> getMappingRegistry() {
        return mappingRegistry;
    }

    /**
     * 🛠️ Sets a custom {@link MappingRegistry} instance.
     *
     * <p>Useful for replacing the default registry with a preconfigured or extended version.</p>
     *
     * @param mappingRegistry the new mapping registry to use
     */
    public void setMappingRegistry(MappingRegistry<H> mappingRegistry) {
        this.mappingRegistry = mappingRegistry;
    }

    /**
     * ⚙️ Initializes the mapping with context and interceptors.
     *
     * @param context current web bean context
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void initialize(WebBeanContext context) {
        setHandlerInterceptorsRegistry(context.getBean(HandlerInterceptorRegistry.class));
        setMappingRegistry(context.getBean(MappingRegistry.class));
        super.initialize(context);
    }

}
