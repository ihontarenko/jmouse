package org.jmouse.cad.live;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;
import org.jmouse.cad.jpa.catalogue.DocumentStore;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.DocumentAccess;
import org.jmouse.cad.store.catalogue.DocumentReading;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 🔌 The live-circuit socket beside jmCAD's API. Imported by a product, never auto-configured:
 *
 * <pre>{@code
 * @Configuration
 * @Import({CadManagementConfiguration.class, CadLiveConfiguration.class})
 * class CadAccessConfiguration { … }
 * }</pre>
 *
 * <h3>Why imported rather than auto-configured</h3>
 *
 * <p>The socket runs circuits as whoever its token names, and no access rule stands in front of it — its handshake is the
 * whole gate, and every message is then decided by the host's {@code CadAuthority} as that actor. An endpoint like that
 * should exist because somebody typed {@code @Import}. It also cannot work without the product's
 * {@link LiveAuthenticator}, so an auto-configuration would have nothing to offer a product without one but a failure to
 * boot.</p>
 *
 * <h3>⚠️ What the product owes</h3>
 *
 * <p>A {@link LiveAuthenticator}; a security chain that lets {@link CadLiveRoutes#PATH} through (the token rides as the
 * {@code bearer.<token>} subprotocol, since a browser sets no header on a handshake); and a dev-server proxy that forwards
 * WebSockets.</p>
 *
 * <p>Registered with the servlet container's own WebSocket support after every initializer has run — the container is a
 * servlet-context attribute only from then on. ⚠️ Tomcat hands {@code modifyHandshake} a configuration copied per
 * session, so the principal it records belongs to that socket alone.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ServerContainer.class)
public class CadLiveConfiguration {

    @Bean
    public LiveCircuits cadLiveCircuits(CadCatalogue catalogue, DocumentAccess access, DocumentStore documents,
                                        DocumentReading reading, CadAuthority authority, CadPlaces places,
                                        PlatformTransactionManager transactions) {
        TransactionTemplate reads = new TransactionTemplate(transactions);
        reads.setReadOnly(true);
        return new LiveCircuits(catalogue, access, documents, reading, authority, places, reads);
    }

    @Bean
    public ServletListenerRegistrationBean<ServletContextListener> cadLiveCircuitSocket(
            LiveCircuits circuits, ObjectMapper mapper, ObjectProvider<LiveAuthenticator> authenticator, Environment environment) {
        LiveAuthenticator available = authenticator.getIfAvailable();
        if (available == null) {
            throw new IllegalStateException("The jmCAD live socket needs a LiveAuthenticator bean: it is what turns the token "
                    + "a handshake offers into the actor the circuit runs as.");
        }
        String path = environment.resolveRequiredPlaceholders(CadLiveRoutes.PATH);
        return new ServletListenerRegistrationBean<>(new SocketRegistration(path, circuits, mapper, available));
    }

    /** Adds the endpoint to the container once the servlet context is up. */
    private record SocketRegistration(String path, LiveCircuits circuits, ObjectMapper mapper,
                                      LiveAuthenticator authenticator) implements ServletContextListener {

        @Override
        public void contextInitialized(ServletContextEvent event) {
            ServerContainer container = (ServerContainer) event.getServletContext().getAttribute(ServerContainer.class.getName());
            if (container == null) {
                throw new IllegalStateException("The servlet container has no WebSocket support; live circuits need it.");
            }
            ServerEndpointConfig configuration = ServerEndpointConfig.Builder
                    .create(LiveCircuitSocket.class, path)
                    .subprotocols(List.of(LiveCircuitSocket.PROTOCOL))
                    .configurator(new LiveCircuitSocket.Configurator(circuits, mapper, authenticator))
                    .build();
            try {
                container.addEndpoint(configuration);
            } catch (DeploymentException refused) {
                throw new IllegalStateException("The live-circuit socket could not be registered at " + path + ".", refused);
            }
        }
    }
}
