package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.InitializingBean;
import org.jmouse.core.Sorter;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.request.RequestAttributesHolder;
import org.jmouse.web.http.request.RequestPath;
import org.jmouse.web.http.response.HttpServletHeadersBuffer;
import org.jmouse.web.mvc.adapter.RequestHttpHandler;
import org.jmouse.web.mvc.cors.*;
import org.jmouse.web.mvc.cors.preflight.PreflightCorsHandler;
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
     * Registry of path-based CORS mappings (expected to be provided via DI)
     */
    private CorsMappingRegistry corsMappingRegistry;

    /**
     * Resolves CORS policy from annotations (e.g., @CorsMapping)
     */
    private final CorsConfigurationProvider corsProvider = new AnnotationCorsConfigurationProvider();

    /**
     * Strategy that applies the resolved CORS policy to requests/responses
     */
    private final CorsProcessor corsProcessor = new WebCorsProcessor();

    /**
     * 🔎 Resolves the handler for the current request and attaches interceptors.
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Resolve a {@link MappedHandler} (or return {@code null} if none).</li>
     *   <li>Collect, sort, and attach global {@link HandlerInterceptor}s.</li>
     *   <li>If the request is a CORS request and a path mapping exists,
     *       wrap the handler with CORS interception (and preflight handling when applicable).</li>
     * </ol>
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

        if (Cors.isCorsRequest(request)) {
            RequestPath requestPath = RequestAttributesHolder.getRequestPath();
            String      mappingPath = requestPath.path();
            if (corsMappingRegistry.hasMapping(mappingPath)) {
                container = getWrappedHandler(container, corsMappingRegistry.lookup(mappingPath), request);
            }
        }

        return container;
    }

    /**
     * Wraps a resolved {@link Handler} with CORS behavior for the current exchange.
     *
     * <p>Always inserts a {@link CorsInterceptor} at index {@code 0} so it runs before
     * user interceptors. For preflight requests, replaces the underlying handler with a
     * lightweight {@code PreflightCorsHandler} and preserves the interceptor chain.</p>
     *
     * @param container     the resolved handler container to wrap
     * @param configuration effective CORS configuration for the route
     * @param request       current HTTP request
     * @return a handler container augmented with CORS interception (and preflight handling)
     */
    public Handler getWrappedHandler(Handler container, CorsConfiguration configuration, HttpServletRequest request) {
        boolean preflight = Cors.isPreflight(request);

        // DOUBLE CHECK!
        // If request is not CORS return original handler container
        if (!Cors.isCorsRequest(request)) {
            return container;
        }

        // Ensure CORS runs first
        container.addInterceptor(0, new CorsInterceptor(preflight, configuration, getCorsProcessor()));

        if (preflight) {
            // Replace the handler but keep interceptor order (CORS first, then user interceptors)
            List<HandlerInterceptor> interceptors  = new ArrayList<>(container.getInterceptors());
            RequestHttpHandler       handler       = new PreflightCorsHandler();
            SimpleMappedHandler      mappedHandler = new SimpleMappedHandler(handler);
            // Override handler with CORS behavior
            container = new Handler(mappedHandler);
            interceptors.forEach(container::addInterceptor);
        }

        return container;
    }

    /**
     * Returns the active provider that resolves CORS configuration
     * (e.g., from {@link CorsMapping} annotations).
     *
     * @return non-null CORS configuration provider
     * @see AnnotationCorsConfigurationProvider
     */
    public CorsConfigurationProvider getCorsProvider() {
        return corsProvider;
    }

    /**
     * Returns the CORS processor strategy responsible for emitting
     * {@code Access-Control-*} headers.
     *
     * @return non-null CORS processor
     * @see WebCorsProcessor
     */
    public CorsProcessor getCorsProcessor() {
        return corsProcessor;
    }

    /**
     * Returns the registry of path-based CORS mappings.
     *
     * @return the registry instance, or {@code null} if not configured/injected
     * @see CorsMappingRegistry
     */
    public CorsMappingRegistry getCorsMappingRegistry() {
        return corsMappingRegistry;
    }

    /**
     * Sets the registry of path-based CORS mappings.
     *
     * <p>Passing {@code null} disables path-based CORS lookup (only annotation-based
     * or default policies, if any, will apply). The instance is stored as-is and not
     * defensively copied.</p>
     *
     * @param corsMappingRegistry the registry to use, or {@code null} to disable
     * @see CorsMappingRegistry
     */
    public void setCorsMappingRegistry(CorsMappingRegistry corsMappingRegistry) {
        this.corsMappingRegistry = corsMappingRegistry;
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
        setCorsMappingRegistry(context.getBean(CorsMappingRegistry.class));
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
