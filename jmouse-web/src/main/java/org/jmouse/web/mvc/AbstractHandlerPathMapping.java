package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.mvc.mapping.RequestHttpHandlerMapping;
import org.jmouse.web.mvc.routing.MappingRegistration;
import org.jmouse.web.mvc.routing.MappingRegistry;
import org.jmouse.web.mvc.routing.MappingCriteria;
import org.jmouse.core.AnsiColors;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.core.MethodParameter;
import org.jmouse.web.http.request.RequestRoute;
import org.jmouse.web.mvc.routing.condition.HttpMethodCondition;
import org.jmouse.web.mvc.routing.condition.RequestPathCondition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.jmouse.core.Streamable.of;

/**
 * 🧭 Abstract base class for route-based handler mappings.
 * <p>
 * Matches incoming HTTP requests against {@link RoutePath} routes and provides
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
 *   <li>Typed route matching via {@link RoutePath} and {@link RouteMatch}</li>
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
     * Resolves the {@link H} handler for the incoming request.
     *
     * @param request current HTTP request
     * @return the matched handler, or {@code null} if no match found
     */
    @Override
    protected MappedHandler doGetHandler(HttpServletRequest request) {
        return getMappedHandler(request);
    }

    /**
     * ➕ Registers a route and its corresponding handler.
     *
     * @param route   path matched, e.g. {@code /user/{id}}
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
        RequestRoute    requestRoute = RequestRoute.ofRequest(request);
        MappingCriteria winner       = getWinner(requestRoute);

        if (winner == null) {
            Set<HttpMethod> methods = getAllowedMethods(requestRoute);

            if (!methods.isEmpty()) {
                if (HttpMethod.OPTIONS.matches(requestRoute.method())) {
                    methods.add(HttpMethod.OPTIONS);
                    return getOptionsHttpRequestHandler(methods);
                }

                throw new MethodNotAllowedException(
                        methods, "HTTP method '%s' for path '%s' is disallowed. Allowed only '%s'.".formatted(
                                requestRoute.method(), requestRoute.requestPath().path(), of(methods).joining(", ")));
            }

            LOGGER.info(AnsiColors.colorize(
                    "❌\uD83E\uDD7A ${RED_BOLD_BRIGHT}UNMATCHED:${RESET} ${YELLOW_BOLD_BRIGHT}%s${RESET}", requestRoute));

            return null;
        }

        MappingRegistration<H> registration = mappingRegistry.getRegistration(winner);
        H                      handler      = registration.handler();

        if (!supportsMappedHandler(handler)) {
            return null;
        }

        Route         route         = winner.getRoute();
        RouteMatch    match         = route.pathPattern().match(requestRoute.requestPath().path());
        MappingResult mappingResult = MappingResult.of(match, route);
        MappedHandler mappedHandler = new RouteMappedHandler(handler, mappingResult, getReturnParameter(handler));

        request.setAttribute(ROUTE_MATCH_ATTRIBUTE, match);
        request.setAttribute(ROUTE_PRODUCIBLE_ATTRIBUTE, route.produces());

        LOGGER.info(AnsiColors.colorize(
                "✅🔥 ${BLUE_BOLD_BRIGHT}MATCHED:${RESET} ${GREEN_BOLD_BRIGHT}%s${RESET}", match));

        return mappedHandler;
    }

    private MappedHandler getOptionsHttpRequestHandler(Set<HttpMethod> allowedMethods) {
        OptionsRequestHttpHandler handler = new OptionsRequestHttpHandler(allowedMethods);
        return new RouteMappedHandler(handler, MappingResult.EMPTY, RequestHttpHandlerMapping.METHOD_PARAMETER);
    }

    private Set<HttpMethod> getAllowedMethods(RequestRoute requestRoute) {
        Set<HttpMethod> methods = new LinkedHashSet<>();

        for (MappingCriteria criterion : getRequestPathMappings(requestRoute)) {
            HttpMethodCondition methodCondition = criterion.getMatcher(HttpMethodCondition.class);
            if (methodCondition != null) {
                methods.addAll(methodCondition.getMethods());
            }
        }

        methods.add(HttpMethod.OPTIONS);
        methods.remove(HttpMethod.TRACE);

        return methods;
    }

    /**
     * 🧩 Extracts the method parameter metadata from the given handler.
     *
     * <p>Used by argument resolvers to access the parameter information
     * of the controller method during request processing.
     *
     * @param handler the handler object containing the controller method
     * @return a {@link MethodParameter} describing the target method parameter
     */
    abstract protected MethodParameter getReturnParameter(H handler);

    /**
     * 🥇 Selects the most specific and applicable {@link MappingCriteria}
     * that matches the incoming {@link RequestRoute}.
     *
     * <p>Algorithm:</p>
     * <ul>
     *   <li>Filter all criteria that {@link MappingCriteria#matches(RequestRoute) match} the request</li>
     *   <li>Sort by specificity using {@code compare(a, b, route)} (most specific first)</li>
     *   <li>Return the top candidate</li>
     * </ul>
     *
     * @param requestRoute parsed request route from the incoming HTTP request
     * @return best matching {@link MappingCriteria}, or {@code null} if none matched
     * @see MappingCriteria#matches(RequestRoute)
     */
    private MappingCriteria getWinner(RequestRoute requestRoute) {
        List<MappingCriteria> candidates = new ArrayList<>();

        for (MappingCriteria mapping : mappingRegistry.getMappingCriteria()) {
            if (mapping.matches(requestRoute)) {
                candidates.add(mapping);
            }
        }

        // Most specific first (note the negation to reverse natural order)
        candidates.sort((a, b) -> -1 * a.compare(b, requestRoute));

        return candidates.isEmpty() ? null : candidates.getFirst();
    }

    /**
     * 🧭 Returns mappings whose {@link RequestPathCondition} matches the request path.
     *
     * <p>Use this to pre-filter by path only (e.g., for diagnostics or to distinguish
     * 404 vs 405), without evaluating other conditions like method, headers, etc.</p>
     *
     * <p>The returned list preserves registration order and is not sorted by specificity.</p>
     *
     * @param requestRoute parsed request route from the incoming HTTP request
     * @return list of path-matching {@link MappingCriteria} (possibly empty, never {@code null})
     * @see RequestPathCondition#matches(RequestRoute)
     */
    private List<MappingCriteria> getRequestPathMappings(RequestRoute requestRoute) {
        List<MappingCriteria> candidates = new ArrayList<>();

        for (MappingCriteria mapping : mappingRegistry.getMappingCriteria()) {
            RequestPathCondition pathCondition = mapping.getMatcher(RequestPathCondition.class);
            if (pathCondition.matches(requestRoute)) {
                candidates.add(mapping);
            }
        }

        return candidates;
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
