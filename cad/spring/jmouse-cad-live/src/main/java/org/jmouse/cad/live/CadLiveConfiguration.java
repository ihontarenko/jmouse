package org.jmouse.cad.live;

import jakarta.servlet.ServletContextListener;
import jakarta.websocket.server.ServerContainer;
import org.jmouse.cad.folder.DocumentStore;
import org.jmouse.cad.management.ExchangeController;
import org.jmouse.cad.management.RoutingController;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.DocumentAccess;
import org.jmouse.cad.store.catalogue.DocumentReading;
import org.jmouse.websocket.SocketEndpoints;
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
 * 🔌 The live socket beside jmCAD's API, one endpoint ({@code jmouse-websocket}) holding two conversations, each by the
 * subprotocol a reader asks for: {@code cad.live}, a running circuit; {@code cad.pcb}, an open pcb's session with the router
 * and the checks. Imported by a product, never auto-configured:
 *
 * <pre>{@code
 * @Configuration
 * @Import({CadManagementConfiguration.class, CadLiveConfiguration.class})
 * class CadAccessConfiguration { … }
 * }</pre>
 *
 * <h3>Why imported rather than auto-configured</h3>
 *
 * <p>The socket works as whoever its token names, and no access rule stands in front of it — its handshake is the whole
 * gate, and every message is then decided by the host's {@code CadAuthority} as that actor. An endpoint like that should exist
 * because somebody typed {@code @Import}. It also cannot work without the product's {@link LiveAuthenticator}, so an
 * auto-configuration would have nothing to offer a product without one but a failure to boot.</p>
 *
 * <h3>⚠️ What the product owes</h3>
 *
 * <p>A {@link LiveAuthenticator}; a security chain that lets {@link CadLiveRoutes#PATH} through (the token rides as the
 * {@code bearer.<token>} subprotocol, since a browser sets no header on a handshake); and a dev-server proxy that forwards
 * WebSockets.</p>
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
    public ServletListenerRegistrationBean<ServletContextListener> cadLiveSocket(
            LiveCircuits circuits, RoutingController routing, ExchangeController exchange, ObjectMapper mapper,
            ObjectProvider<LiveAuthenticator> authenticator, Environment environment) {
        LiveAuthenticator available = authenticator.getIfAvailable();
        if (available == null) {
            throw new IllegalStateException("The jmCAD live socket needs a LiveAuthenticator bean: it is what turns the token "
                    + "a handshake offers into the actor the socket works as.");
        }
        String path = environment.resolveRequiredPlaceholders(CadLiveRoutes.PATH);
        return new ServletListenerRegistrationBean<>(new SocketEndpoints(path, List.of(
                new LiveCircuitProtocol(circuits, mapper),
                new LivePcbProtocol(new PcbAsks(routing, exchange, mapper), mapper)), available));
    }
}
