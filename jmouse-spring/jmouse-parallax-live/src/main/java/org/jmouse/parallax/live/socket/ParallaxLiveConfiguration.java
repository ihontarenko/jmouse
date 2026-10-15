package org.jmouse.parallax.live.socket;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;
import org.jmouse.parallax.authority.ParallaxAuthority;
import org.jmouse.parallax.authority.ParallaxPlaces;
import org.jmouse.parallax.live.LiveAuthenticator;
import org.jmouse.parallax.store.catalogue.Catalogue;
import org.jmouse.parallax.store.catalogue.ComponentModels;
import org.jmouse.parallax.store.catalogue.ObjectDocuments;
import org.jmouse.parallax.store.support.ParallaxJson;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * 🔌 The live-circuit socket. Imported by a product, never auto-configured:
 *
 * <pre>{@code
 * @Configuration
 * @Import(ParallaxLiveConfiguration.class)
 * class ParallaxAccessConfiguration { … }
 * }</pre>
 *
 * <h3>Why imported rather than auto-configured</h3>
 *
 * <p>The socket runs circuits as whoever its token names, and no access rule stands in front of it — its handshake
 * is the whole gate. An endpoint like that should exist because somebody typed {@code @Import}, the same way the
 * REST API and the vault's management API do, and not because a starter happened to be on the classpath. It also
 * cannot work without the product's {@link LiveAuthenticator}, so an auto-configuration would have nothing to offer
 * a product that has not written one but a failure to boot.</p>
 *
 * <h3>⚠️ What the product owes</h3>
 *
 * <p>A {@link LiveAuthenticator}; a security chain that lets {@link ParallaxLiveRoutes#PATH} through (the token rides
 * as the {@code bearer.<token>} subprotocol, since a browser sets no header on a handshake, so a filter that reads that
 * subprotocol authenticates the handshake exactly as the socket will); and a dev-server proxy that forwards WebSockets.</p>
 *
 * <p>Registered with the servlet container's own WebSocket support (Tomcat's, which Boot already installs), after
 * every initializer has run — the container is a servlet-context attribute only from then on. ⚠️ Tomcat hands
 * {@code modifyHandshake} a configuration copied per session, so the principal it records belongs to that socket
 * alone.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ServerContainer.class)
public class ParallaxLiveConfiguration {

    @Bean
    public LiveCircuits parallaxLiveCircuits(Catalogue catalogue, ComponentModels components, ObjectDocuments documents,
                                             ParallaxAuthority authority, ParallaxPlaces places) {
        return new LiveCircuits(catalogue, components, documents, authority, places);
    }

    @Bean
    public ServletListenerRegistrationBean<ServletContextListener> parallaxLiveCircuitSocket(
            LiveCircuits circuits, ParallaxJson json, ObjectProvider<LiveAuthenticator> authenticator,
            Environment environment) {
        LiveAuthenticator available = authenticator.getIfAvailable();

        if (available == null) {
            throw new IllegalStateException("The Parallax live socket needs a LiveAuthenticator bean: it is what turns "
                    + "the token a handshake offers into the actor the circuit runs as.");
        }

        String path = environment.resolveRequiredPlaceholders(ParallaxLiveRoutes.PATH);

        return new ServletListenerRegistrationBean<>(new SocketRegistration(path, circuits, json, available));
    }

    /** Adds the endpoint to the container once the servlet context is up. */
    private record SocketRegistration(String path, LiveCircuits circuits, ParallaxJson json,
                                      LiveAuthenticator authenticator) implements ServletContextListener {

        @Override
        public void contextInitialized(ServletContextEvent event) {
            ServerContainer container = (ServerContainer) event.getServletContext()
                    .getAttribute(ServerContainer.class.getName());

            if (container == null) {
                throw new IllegalStateException("The servlet container has no WebSocket support; live circuits need it.");
            }

            ServerEndpointConfig configuration = ServerEndpointConfig.Builder
                    .create(LiveCircuitSocket.class, path)
                    .subprotocols(List.of(LiveCircuitSocket.PROTOCOL))
                    .configurator(new LiveCircuitSocket.Configurator(circuits, json.mapper(), authenticator))
                    .build();

            try {
                container.addEndpoint(configuration);
            } catch (DeploymentException refused) {
                throw new IllegalStateException("The live-circuit socket could not be registered at " + path + ".",
                        refused);
            }
        }
    }
}
