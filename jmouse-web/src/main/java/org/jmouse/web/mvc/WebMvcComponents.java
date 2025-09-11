package org.jmouse.web.mvc;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.core.MediaType;
import org.jmouse.core.MediaTypeFactory;
import org.jmouse.web.mvc.resource.ResourceHandlerRegistry;
import org.jmouse.web.mvc.resource.ResourceUrlResolver;
import org.jmouse.web.negotiation.FallbackMediaTypeLookup;
import org.jmouse.web.negotiation.MediaTypeManager;

import java.util.Map;

import static org.jmouse.core.Charset.UTF_8;

/**
 * 🧩 Defines core MVC beans for the jMouse framework.
 *
 * <p>📦 Used during context initialization to register
 * routing, interceptors, and content negotiation facilities.</p>
 *
 * <p>Acts similar to a Spring-style configuration class, but driven by
 * {@link BeanFactories} and jMouse’s lightweight bean system.</p>
 *
 * <p>⚙️ Key responsibilities:</p>
 * <ul>
 *   <li>📂 Register static resource handlers</li>
 *   <li>🛡️ Register handler interceptors</li>
 *   <li>📑 Provide media type negotiation support</li>
 *   <li>🪶 Configure fallback lookups and defaults</li>
 * </ul>
 *
 * @author Ivan Hontarenko
 */
@BeanFactories
public class WebMvcComponents {

    /**
     * 🔗 Resolver that rewrites resource URLs (e.g. appending versioning).
     *
     * @return a new {@link ResourceUrlResolver} bean
     */
    @Bean
    public ResourceUrlResolver resourceUrlResolver() {
        return new ResourceUrlResolver();
    }

    /**
     * 📂 Registry of static resource handlers.
     *
     * <p>Allows mapping URL paths (e.g. {@code /static/**})
     * to physical locations (classpath or filesystem).</p>
     *
     * @return {@link ResourceHandlerRegistry} bean
     */
    @Bean
    public ResourceHandlerRegistry resourceHandlerConfiguration() {
        return new ResourceHandlerRegistry();
    }

    /**
     * 🛡️ Registry of handler interceptors.
     *
     * <p>Stores MVC interceptors that can apply cross-cutting
     * behavior (e.g. logging, auth, metrics) around handler execution.</p>
     *
     * @return {@link HandlerInterceptorRegistry} bean
     */
    @Bean
    public HandlerInterceptorRegistry handlerInterceptorRegistry() {
        return new HandlerInterceptorRegistry();
    }

    /**
     * 📑 Manager for request/response media type negotiation.
     *
     * <p>Combines {@link org.jmouse.web.negotiation.MediaTypeLookup}
     * strategies (e.g. {@code Accept} header, fallback) to determine
     * the most appropriate {@link MediaType}.</p>
     *
     * @return {@link MediaTypeManager} bean
     */
    @Bean
    public MediaTypeManager mediaTypeManager() {
        return new MediaTypeManager();
    }

    /**
     * 🏭 Factory for resolving {@link MediaType} by file extension.
     *
     * <p>Loads mappings from {@code mime.types} and provides quick lookup
     * for resource-based type resolution (e.g. {@code .png → image/png}).</p>
     *
     * @return {@link MediaTypeFactory} bean
     */
    @Bean
    public MediaTypeFactory mediaTypeFactory() {
        return new MediaTypeFactory();
    }

    /**
     * 🪶 Configures the {@link FallbackMediaTypeLookup}.
     *
     * <p>Provides a default fallback media type
     * {@code application/xml; charset=UTF-8; x-debug=fallback}.</p>
     */
    @Bean
    public static class ConfigureFallbackLookup implements BeanConfigurer<FallbackMediaTypeLookup> {

        /** 🔧 Debug parameter used in fallback media type. */
        public static final String DEBUG_PARAMETER_VALUE = "fallback";

        /**
         * ⚙️ Customize the given {@link FallbackMediaTypeLookup}
         * with a predefined {@link MediaType}.
         *
         * @param fallback the fallback lookup to configure
         */
        @Override
        public void configure(FallbackMediaTypeLookup fallback) {
            Map<String, String> parameters = Map.of(
                    MediaType.PARAMETER_NAME_CHARSET, UTF_8.getName(),
                    MediaType.PARAMETER_NAME_DEBUG, DEBUG_PARAMETER_VALUE
            );
            fallback.setFallback(new MediaType("application", "xml", parameters));
        }
    }
}
