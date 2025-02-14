package org.jmouse.web.server;

import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.Qualifier;

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
@BeanFactories("defaultFactories")
public record WebServerConfig(@Qualifier("localPort") int port, @Qualifier("default") WebServers name) {
}
