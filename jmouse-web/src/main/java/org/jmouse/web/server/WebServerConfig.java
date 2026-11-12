package org.jmouse.web.server;

/**
 * Configuration record for a web server instance.
 * <p>
 * This record encapsulates the essential configuration parameters required to start
 * a web server, including its host, port, and name.
 * </p>
 *
 * @param port the port on which the server will listen
 * @param name the name of the web server instance
 */
public record WebServerConfig(int port, WebServers name, String temporary) {
    public WebServerConfig {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Web server port must be between 0 and 65535");
        }
        java.util.Objects.requireNonNull(name, "name");
    }
}
