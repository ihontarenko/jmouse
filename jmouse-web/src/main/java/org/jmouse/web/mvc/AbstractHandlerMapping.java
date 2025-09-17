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
import org.jmouse.web.mvc.cors.*;
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

        if (Cors.isCorsRequest(request)) {
            RequestPath requestPath = RequestAttributesHolder.getRequestPath();
            String path = requestPath.path();
            if (getCorsMappingRegistry().hasMapping(path)) {
                CorsConfiguration corsConfiguration = getCorsMappingRegistry().lookup(path);

                if (Cors.isPreflight(request)) {
                    Headers responseHeaders = new Headers();
                    Headers requestHeaders = RequestAttributesHolder.getRequestRoute().headers();
                    getCorsProcessor().handleRequest(corsConfiguration, requestHeaders, responseHeaders, true);
                    return null;
                }

                System.out.println(corsConfiguration);
            }
        }

        if (!interceptors.isEmpty()) {
            Sorter.sort(interceptors);
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
        corsMappingRegistry = context.getBean(CorsMappingRegistry.class);
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
