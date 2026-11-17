package org.jmouse.websocket;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;

import java.util.List;

/**
 * 🛰️ An endpoint at a path, holding these protocols, added to the servlet container's own WebSocket support once the servlet
 * context is up — the container is a servlet-context attribute only from then on. A host registers it as a servlet context
 * listener:
 *
 * <pre>{@code
 * new ServletListenerRegistrationBean<>(new SocketEndpoints("/jmouse/cad/live", List.of(circuits, pcbs), authenticator));
 * }</pre>
 *
 * <p>⚠️ What the host owes: a security chain that lets the path through — the socket's handshake is its whole gate, every
 * message then decided as the actor the token names —, and a dev-server proxy that forwards WebSockets.</p>
 */
public final class SocketEndpoints implements ServletContextListener {

    private final String               path;
    private final List<SocketProtocol> protocols;
    private final SocketAuthenticator  authenticator;

    public SocketEndpoints(String path, List<SocketProtocol> protocols, SocketAuthenticator authenticator) {
        if (protocols.isEmpty()) {
            throw new IllegalArgumentException("An endpoint at " + path + " needs at least one protocol");
        }
        this.path = path;
        this.protocols = List.copyOf(protocols);
        this.authenticator = authenticator;
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServerContainer container = (ServerContainer) event.getServletContext().getAttribute(ServerContainer.class.getName());
        if (container == null) {
            throw new IllegalStateException("The servlet container has no WebSocket support; " + path + " needs it.");
        }
        ServerEndpointConfig configuration = ServerEndpointConfig.Builder
                .create(ProtocolSocket.class, path)
                .subprotocols(protocols.stream().map(SocketProtocol::name).toList())
                .configurator(new ProtocolSocket.Configurator(protocols, authenticator))
                .build();
        try {
            container.addEndpoint(configuration);
        } catch (DeploymentException refused) {
            throw new IllegalStateException("The socket endpoint could not be registered at " + path + ".", refused);
        }
    }
}
