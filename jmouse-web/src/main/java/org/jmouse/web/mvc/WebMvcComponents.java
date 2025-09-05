package org.jmouse.web.mvc;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.core.MediaType;
import org.jmouse.web.negotiation.FallbackMediaTypeLookup;
import org.jmouse.web.negotiation.MediaTypeManager;

import java.util.Map;

import static org.jmouse.core.Charset.UTF_8;

/**
 * 🧩 Defines core MVC beans for the jMouse framework.
 *
 * <p>📦 Used during context initialization to register routing and interceptors.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@BeanFactories
public class WebMvcComponents {

    @Bean
    public HandlerInterceptorRegistry handlerInterceptorRegistry() {
        return new HandlerInterceptorRegistry();
    }

    @Bean
    public MediaTypeManager mediaTypeManager() {
        return new MediaTypeManager();
    }

    @Bean
    public static class ConfigureFallbackLookup implements BeanConfigurer<FallbackMediaTypeLookup> {

        @Override
        public void configure(FallbackMediaTypeLookup fallback) {
            Map<String, String> parameters = Map.of(
                    MediaType.PARAMETER_NAME_CHARSET, UTF_8.getName(),
                    MediaType.PARAMETER_NAME_DEBUG, "fallback");
            fallback.setFallback(new MediaType("application", "xml", parameters));
        }

    }

}
