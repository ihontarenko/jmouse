package org.jmouse.web.server.tomcat;

import java.util.ArrayList;
import java.util.List;
import org.apache.catalina.Container;
import org.apache.catalina.Engine;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Service;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.util.ServerInfo;
import org.jmouse.web.server.WebServer;
import org.jmouse.web.server.WebServerException;

/** Owns an embedded Tomcat instance. stop/close are terminal; restart uses a new server instance. */
public class TomcatWebServer implements WebServer {

    private final Tomcat tomcat = new Tomcat();
    private Thread awaitThread;
    private boolean running;
    private boolean closed;

    @Override
    public synchronized void start() throws WebServerException {
        if (closed) {
            throw new WebServerException("Cannot start a closed web server");
        }
        if (running) {
            return;
        }
        try {
            // Create the connector before startup, including for programmatic applications.
            tomcat.getConnector();
            tomcat.start();
            ensureAllContextsStarted();
            if (!tomcat.getConnector().getState().isAvailable()) {
                throw new WebServerException("HTTP connector failed to start");
            }
            running = true;
            awaitThread = new Thread(() -> tomcat.getServer().await(), "Tomcat-AsyncAwait");
            awaitThread.setContextClassLoader(getClass().getClassLoader());
            awaitThread.setDaemon(false);
            awaitThread.start();
            LOGGER.info("Web server started on port {}", port());
        } catch (Exception | Error failure) {
            try {
                close();
            } catch (RuntimeException | Error cleanupFailure) {
                if (failure != cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw propagate("Web server startup failed", failure);
        }
    }

    @Override
    public synchronized void stop() throws WebServerException {
        close();
    }

    @Override
    public synchronized void close() throws WebServerException {
        if (closed) {
            return;
        }
        Throwable failure = null;
        try {
            LifecycleState state = tomcat.getServer().getState();
            if (
                state != LifecycleState.NEW &&
                state != LifecycleState.STOPPED &&
                state != LifecycleState.DESTROYED
            ) {
                tomcat.stop();
            }
        } catch (Exception | Error exception) {
            failure = exception;
        }
        try {
            tomcat.destroy();
        } catch (Exception | Error exception) {
            failure = append(failure, exception);
        } finally {
            running = false;
            closed = true;
            try {
                joinAwaitThread();
            } catch (RuntimeException exception) {
                failure = append(failure, exception);
            }
        }
        if (failure != null) {
            throw propagate("Web server close failed", failure);
        }
    }

    /** Actual bound HTTP port, including when configured with port zero. */
    public synchronized int port() {
        return tomcat.getConnector().getLocalPort();
    }

    private void joinAwaitThread() {
        Thread awaiting = awaitThread;
        if (awaiting == null || awaiting == Thread.currentThread()) {
            return;
        }
        try {
            awaiting.join(5000);
            if (awaiting.isAlive()) {
                throw new WebServerException("Web server await thread did not terminate");
            }
            awaitThread = null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new WebServerException("Interrupted while closing web server", exception);
        }
    }

    private static Throwable append(Throwable first, Throwable next) {
        if (first == null) {
            return next;
        }
        if (first != next) {
            first.addSuppressed(next);
        }
        return first;
    }

    private static WebServerException propagate(String message, Throwable failure) {
        if (failure instanceof Error error) {
            throw error;
        }
        return failure instanceof WebServerException serverFailure
            ? serverFailure
            : new WebServerException(message, failure);
    }

    private void ensureAllContextsStarted() {
        List<String> failed = new ArrayList<>();
        Service service = tomcat.getService();
        if (service != null && service.getContainer() instanceof Engine engine) {
            for (Container host : engine.findChildren()) {
                for (Container context : host.findChildren()) {
                    if (
                        context instanceof StandardContext standardContext &&
                        standardContext.getState() != LifecycleState.STARTED
                    ) {
                        failed.add(standardContext.getName() + ':' + standardContext.getState().name());
                    }
                }
            }
        }
        if (!failed.isEmpty()) {
            throw new WebServerException("One or more contexts failed to start: " + failed);
        }
    }

    @Override
    public Tomcat server() {
        return tomcat;
    }

    @Override
    public String name() {
        return ServerInfo.getServerInfo();
    }
}
