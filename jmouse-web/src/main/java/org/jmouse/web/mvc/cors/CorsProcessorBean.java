package org.jmouse.web.mvc.cors;

import org.jmouse.beans.annotation.Bean;

/**
 * 📦 DI adapter that exposes the default {@link WebCorsProcessor} as a bean.
 *
 * <p>Registers a processor under the bean name {@code "corsProcessor"} so it can be
 * injected wherever CORS handling is required.</p>
 *
 * @see WebCorsProcessor
 * @see CorsMappingRegistry
 * @see CorsConfiguration
 */
@Bean("corsProcessor")
public class CorsProcessorBean extends WebCorsProcessor {

    /**
     * 📚 Bean providing a default {@link CorsMappingRegistry}.
     *
     * <p>Declared as a {@code static} nested class to avoid capturing the enclosing
     * instance and to allow the DI container to construct it independently.</p>
     *
     * <p>Registered under the bean name {@code "corsMappingRegistry"}.</p>
     *
     * @see CorsMappingRegistry
     */
    @Bean("corsMappingRegistry")
    public static class CorsMappingRegistryBean extends CorsMappingRegistry { }
}
