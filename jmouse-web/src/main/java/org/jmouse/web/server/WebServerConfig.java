package org.jmouse.web.server;

/**
 * Configuration record for a web server instance.
 * <p>
 * This record encapsulates the essential configuration parameters required to start
 * a web server, including its host, port, and name.
 * </p>
 *
 * @param host the hostname or IP address the server should bind to
 * @param port the port on which the server will listen
 * @param name the name of the web server instance
 */
public record WebServerConfig(int port, WebServers name) {
}
