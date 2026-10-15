package org.jmouse.parallax.live.socket;

import org.jmouse.parallax.live.LiveAuthenticator;
import org.jmouse.parallax.live.LivePrincipal;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * {@link ParallaxLiveRoutes#PATH}: one running circuit per socket. The handshake carries the reader's bearer token as
 * the {@code bearer.<token>} subprotocol beside {@code parallax.live} — a browser cannot set a header on it — and a
 * socket without a valid token is closed at once; what the reader may run is then checked per message, as the
 * principal the host's {@link LiveAuthenticator} found for the token.
 */
final class LiveCircuitSocket extends Endpoint {

    static final String PROTOCOL = "parallax.live";
    static final String TOKEN_PREFIX = "bearer.";
    private static final int MAXIMUM_PROTOCOLS_LENGTH = 300;
    private static final String PRINCIPAL = "principal";
    /** The largest message a reader may send, in characters: a circuit and its sheet, with room to spare. */
    private static final int MAXIMUM_MESSAGE_CHARACTERS = 2 * 1024 * 1024;

    private final LiveCircuits circuits;
    private final ObjectMapper mapper;
    private LiveRun run;

    LiveCircuitSocket(LiveCircuits circuits, ObjectMapper mapper) {
        this.circuits = circuits;
        this.mapper = mapper;
    }

    @Override
    public void onOpen(Session session, EndpointConfig configuration) {
        Optional<LivePrincipal> principal = Optional.ofNullable((LivePrincipal) session.getUserProperties().get(PRINCIPAL));
        if (principal.isEmpty()) {
            closeQuietly(session, new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Sign in to run a circuit."));
            return;
        }
        run = new LiveRun(circuits, principal.get(), mapper, new LiveOutput() {
            @Override
            public void text(String message) {
                send(session, remote -> remote.sendText(message));
            }

            @Override
            public void binary(ByteBuffer frame) {
                send(session, remote -> remote.sendBinary(frame));
            }
        });
        // A `start` or a `restart` carries the whole circuit, its layout included: the container's default of 8 KiB closed
        // the socket on any sheet of more than a dozen parts ("the decoded text message was too big").
        session.setMaxTextMessageBufferSize(MAXIMUM_MESSAGE_CHARACTERS);
        session.addMessageHandler(String.class, (MessageHandler.Whole<String>) run::receive);
    }

    @Override
    public void onClose(Session session, CloseReason reason) {
        if (run != null) {
            run.close();
        }
    }

    @Override
    public void onError(Session session, Throwable failure) {
        if (run != null) {
            run.close();
        }
    }

    /** One send on the session's blocking remote. */
    private interface Sending {
        void to(RemoteEndpoint.Basic remote) throws IOException;
    }

    private static void send(Session session, Sending sending) {
        if (!session.isOpen()) {
            return;
        }
        synchronized (session) {
            try {
                sending.to(session.getBasicRemote());
            } catch (IOException | IllegalStateException gone) {
                closeQuietly(session, new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "The reader is gone."));
            }
        }
    }

    private static void closeQuietly(Session session, CloseReason reason) {
        try {
            session.close(reason);
        } catch (IOException alreadyClosed) {
            // The socket is gone either way.
        }
    }

    /**
     * The token a handshake offers as its {@code bearer.} subprotocol: one header value may list several protocols,
     * separated by commas.
     */
    static Optional<String> tokenOf(List<String> protocolHeaders) {
        return protocolHeaders.stream()
                .filter(protocols -> protocols != null && protocols.length() <= MAXIMUM_PROTOCOLS_LENGTH)
                .flatMap(protocols -> Arrays.stream(protocols.split(",")))
                .map(String::strip)
                .filter(protocol -> protocol.startsWith(TOKEN_PREFIX))
                .map(protocol -> protocol.substring(TOKEN_PREFIX.length()))
                .findFirst();
    }

    /** Authenticates the handshake by its {@code bearer.} subprotocol and builds each socket with the host's services. */
    static final class Configurator extends ServerEndpointConfig.Configurator {

        private final LiveCircuits      circuits;
        private final ObjectMapper      mapper;
        private final LiveAuthenticator authenticator;

        Configurator(LiveCircuits circuits, ObjectMapper mapper, LiveAuthenticator authenticator) {
            this.circuits = circuits;
            this.mapper = mapper;
            this.authenticator = authenticator;
        }

        @Override
        public void modifyHandshake(ServerEndpointConfig configuration, HandshakeRequest request, HandshakeResponse response) {
            List<String> protocols = request.getHeaders().getOrDefault("Sec-WebSocket-Protocol", List.of());
            tokenOf(protocols).flatMap(authenticator::authenticate)
                    .ifPresent(principal -> configuration.getUserProperties().put(PRINCIPAL, principal));
        }

        @Override
        public String getNegotiatedSubprotocol(List<String> supported, List<String> requested) {
            return requested.contains(PROTOCOL) ? PROTOCOL : "";
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getEndpointInstance(Class<T> type) {
            return (T) new LiveCircuitSocket(circuits, mapper);
        }
    }
}
