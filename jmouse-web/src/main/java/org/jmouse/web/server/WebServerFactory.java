package org.jmouse.web.server;

import org.jmouse.web.initializer.WebApplicationInitializer;

/**
 * Factory interface for creating {@link WebServer} instances.
 * <p>
 * Implementations of this interface are responsible for configuring and providing
 * {@link WebServer} instances, optionally initialized with one or more {@link WebApplicationInitializer}s.
 *
 * @see WebServer
 * @see WebApplicationInitializer
 */
public interface WebServerFactory {

    String WEB_SERVER_CONFIGURATION_PATH = "jmouse.web.server";

    /**
     * Returns a {@link WebServer} instance configured with the specified initializers.
     *
     * @param initializers the {@link WebApplicationInitializer}s to configure the web server
     * @return a configured {@link WebServer} instance
     */
    WebServer createWebServer(WebApplicationInitializer... initializers);

}
