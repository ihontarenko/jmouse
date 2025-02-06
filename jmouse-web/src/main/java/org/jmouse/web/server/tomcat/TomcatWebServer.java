package org.jmouse.web.server.tomcat;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.util.ServerInfo;
import org.jmouse.web.server.WebServer;
import org.jmouse.web.server.WebServerException;

public class TomcatWebServer implements WebServer {

    private final Tomcat tomcat;

    public TomcatWebServer() {
        this.tomcat = new Tomcat();
    }

    /**
     * Starts the web server.
     *
     * @throws WebServerException if an error occurs during startup.
     */
    @Override
    public void start() throws WebServerException {
        try {

            LOGGER.info("Web-Server: Starting '{}'...", name());
            tomcat.start();
            LOGGER.info("Web-Server: '{}' started! ", name());
            LOGGER.info("Port: {}, Catalina Base: {}", server().getConnector().getPort(), server().getServer().getCatalinaBase().getAbsolutePath());

            Thread async = new Thread(() -> {
                LOGGER.info("Web-Server: '{}' awaiting! ", name());
                tomcat.getServer().await();
            });
            async.setContextClassLoader(getClass().getClassLoader());
            async.setDaemon(false);
            async.setName("%s-AsyncAwait".formatted(name()));
            async.start();

        } catch (LifecycleException e) {
            stop();
            throw new WebServerException("Error occurred during '" + name() + "' web-server stopping", e);
        }
    }

    /**
     * Stops the web server.
     *
     * @throws WebServerException if an error occurs during shutdown.
     */
    @Override
    public void stop() throws WebServerException {
        try {
            LOGGER.info("{} stopping...", name());
            tomcat.stop();
        } catch (LifecycleException e) {
            throw new WebServerException("Error occurred during '" + name() + "' web-server stopping", e);
        }
    }

    /**
     * Retrieves the underlying server instance.
     *
     * @return the underlying server of type {@code T}.
     */
    @Override
    public Tomcat server() {
        return tomcat;
    }

    /**
     * Retrieves the name of internal server implementation
     *
     * @return the underlying server name.
     */
    @Override
    public String name() {
        return ServerInfo.getServerInfo();
    }

}
