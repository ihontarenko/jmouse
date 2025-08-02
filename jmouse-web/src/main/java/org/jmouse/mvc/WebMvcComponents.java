package org.jmouse.mvc;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.mvc.routing.MappingRegistry;

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

    /**
     * 🚦 Creates the registry for MVC interceptors.
     *
     * @return a new {@link HandlerInterceptorRegistry}
     */
    @Bean
    public HandlerInterceptorRegistry handlerInterceptorRegistry() {
        return new HandlerInterceptorRegistry();
    }

//    /**
//     * 🧭 Creates the main routing registry.
//     *
//     * @return a new {@link MappingRegistry} instance
//     */
//    @Bean
//    public MappingRegistry<?, ?> mappingRegistry() {
//        return new MappingRegistry<>();
//    }

}
