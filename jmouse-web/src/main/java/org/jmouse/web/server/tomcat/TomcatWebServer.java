package org.jmouse.web.server.tomcat;

import org.apache.catalina.*;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.util.ServerInfo;
import org.jmouse.web.server.WebServer;
import org.jmouse.web.server.WebServerException;

import java.util.ArrayList;
import java.util.List;

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

            ensureAllContextsStarted(tomcat);

            LOGGER.info("Web-Server: '{}' started! ", name());
            LOGGER.info("Port: {}, Catalina Base: {}", server().getConnector().getPort(),
                        server().getServer().getCatalinaBase().getAbsolutePath());

            Thread async = new Thread(() -> {
                LOGGER.info("Web-Server: '{}' awaiting! ", name());
                tomcat.getServer().await();
            });

            async.setContextClassLoader(getClass().getClassLoader());
            async.setDaemon(false);
            async.setName("%s-AsyncAwait".formatted(name()));
            async.start();

        } catch (WebServerException e) {
            safeStopOnFailure();
            throw e;
        } catch (Exception e) {
            safeStopOnFailure();
            throw new WebServerException("Unexpected error during '%s' web-server starting".formatted(name()), e);
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

    private void safeStopOnFailure() {
        try {
            tomcat.stop();
        } catch (Exception ex) {
            // swallow, we're already failing startup
        }
    }

    private void ensureAllContextsStarted(Tomcat tomcat) {
        List<String> failed  = new ArrayList<>();
        Service      service = tomcat.getService();

        if (service != null && service.getContainer() instanceof Engine engine) {
            for (Container container : engine.findChildren()) {
                for (Container context : container.findChildren()) {
                    if (context instanceof StandardContext standardContext
                            && standardContext.getState() != LifecycleState.STARTED) {
                        failed.add(standardContext.getName() + ':' + standardContext.getState().name());
                    }
                }
            }
        }

        if (!failed.isEmpty()) {
            throw new WebServerException("One or more contexts failed to start: " + failed);
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
