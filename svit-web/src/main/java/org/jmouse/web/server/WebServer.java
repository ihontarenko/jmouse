package org.jmouse.web.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jmouse.core.reflection.JavaType;

/**
 * A generic interface for managing a web server lifecycle and configuration.
 */
public interface WebServer {

    /**
     * A logger instance for logging server-related events.
     */
    Logger LOGGER = LoggerFactory.getLogger(WebServer.class);

    /**
     * Starts the web server.
     *
     * @throws WebServerException if an error occurs during startup.
     */
    void start() throws WebServerException;

    /**
     * Stops the web server.
     *
     * @throws WebServerException if an error occurs during shutdown.
     */
    void stop() throws WebServerException;

    /**
     * Retrieves the underlying server instance.
     *
     * @return the underlying server of type {@code T}.
     */
    Object server();

    /**
     * Retrieves the name of internal server implementation
     *
     * @return the underlying server name.
     */
    String name();

    /**
     * Configures the web server using the provided {@link Configurer}.
     * <p>
     * Checks that both the configurer and the underlying server are non-null
     * before applying the configuration.
     *
     * @param configurer a functional interface to configure the underlying server
     * @throws WebServerException if the configurer or the underlying server is {@code null}.
     */
    default <T> void configure(Configurer<T> configurer) {
        if (configurer == null) {
            throw new WebServerException("Configurer object must be non-NULL");
        }

        if (server() == null) {
            throw new WebServerException("Underlying server implementation must be non-NULL");
        }

        LOGGER.info("Start configuring '{}' web-server", name());

        JavaType javaType   = JavaType.forInstance(configurer);
        Class<?> serverType = javaType.locate(Configurer.class).getFirst().getRawType();

        LOGGER.info("Type of web-server '{}'", serverType);

        configurer.configure((T) server());
    }

    /**
     * A functional interface for configuring the underlying server.
     *
     * @param <T> the type of the server to configure
     */
    @FunctionalInterface
    interface Configurer<T> {

        /**
         * Configures the server instance of type {@code T}.
         *
         * @param server the server instance to configure
         */
        void configure(T server);
    }
}