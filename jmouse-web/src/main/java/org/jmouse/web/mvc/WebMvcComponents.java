package org.jmouse.web.mvc;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.web.negotiation.MediaTypeManager;

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

}
