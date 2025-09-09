package org.jmouse.web.mvc;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.core.MediaType;
import org.jmouse.web.mvc.resource.ResourceHandlerRegistry;
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
     * 📂 Registry of static resource handlers.
     */
    @Bean
    public ResourceHandlerRegistry resourceHandlerConfiguration() {
        return new ResourceHandlerRegistry();
    }

    /**
     * 🛡️ Registry of handler interceptors.
     */
    @Bean
    public HandlerInterceptorRegistry handlerInterceptorRegistry() {
        return new HandlerInterceptorRegistry();
    }

    /**
     * 📑 Manager for request/response media type negotiation.
     */
    @Bean
    public MediaTypeManager mediaTypeManager() {
        return new MediaTypeManager();
    }

    /**
     * 🪶 Configures the {@link FallbackMediaTypeLookup}.
     *
     * <p>Sets a default fallback media type
     * {@code application/xml; charset=UTF-8; debug=fallback}.</p>
     */
    @Bean
    public static class ConfigureFallbackLookup implements BeanConfigurer<FallbackMediaTypeLookup> {

        /**
         * 🔧 Debug parameter used in fallback media type.
         */
        public static final String DEBUG_PARAMETER_VALUE = "fallback";

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
