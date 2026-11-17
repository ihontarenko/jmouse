package org.jmouse.websocket;

import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🔌 One socket of an endpoint ({@link SocketEndpoints}): its conversation is the protocol the reader asked for. The handshake
 * carries the reader's bearer token as the {@code bearer.<token>} subprotocol beside the protocol's own — a browser cannot set
 * a header on it —, and a socket without a valid token, or asking for no protocol the endpoint holds, is closed at once.
 */
final class ProtocolSocket extends Endpoint {

    static final String TOKEN_PREFIX = "bearer.";
    /** A host's token is whatever the host issues: a signed one runs past a thousand characters. It is still one header. */
    private static final int MAXIMUM_PROTOCOLS_LENGTH = 8192;
    private static final String PRINCIPAL = "principal";

    private final Map<String, SocketProtocol> protocols;
    private SocketConversation                conversation;

    ProtocolSocket(Map<String, SocketProtocol> protocols) {
        this.protocols = protocols;
    }

    @Override
    public void onOpen(Session session, EndpointConfig configuration) {
        Optional<SocketPrincipal> principal = Optional.ofNullable((SocketPrincipal) session.getUserProperties().get(PRINCIPAL));
        if (principal.isEmpty()) {
            closeQuietly(session, new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Sign in first."));
            return;
        }
        SocketProtocol protocol = protocols.get(session.getNegotiatedSubprotocol());
        if (protocol == null) {
            closeQuietly(session, new CloseReason(CloseReason.CloseCodes.PROTOCOL_ERROR, "Ask for one of: " + String.join(", ", protocols.keySet())));
            return;
        }
        session.setMaxTextMessageBufferSize(protocol.maximumMessageCharacters());
        conversation = protocol.open(principal.get(), new SocketOutput() {
            @Override
            public void text(String message) {
                send(session, remote -> remote.sendText(message));
            }

            @Override
            public void binary(ByteBuffer frame) {
                send(session, remote -> remote.sendBinary(frame));
            }
        });
        session.addMessageHandler(String.class, (MessageHandler.Whole<String>) conversation::receive);
    }

    @Override
    public void onClose(Session session, CloseReason reason) {
        closeConversation();
    }

    @Override
    public void onError(Session session, Throwable failure) {
        closeConversation();
    }

    /** The conversation let go once, however the socket ended — a close after an error included. */
    private synchronized void closeConversation() {
        if (conversation != null) {
            conversation.close();
            conversation = null;
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
     * The token a handshake offers as its {@code bearer.} subprotocol: one header value may list several protocols, separated
     * by commas.
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

    /** Authenticates each handshake by its {@code bearer.} subprotocol, agrees the protocol, and builds each socket. */
    static final class Configurator extends ServerEndpointConfig.Configurator {

        private final Map<String, SocketProtocol> protocols;
        private final SocketAuthenticator         authenticator;

        Configurator(List<SocketProtocol> protocols, SocketAuthenticator authenticator) {
            Map<String, SocketProtocol> byName = new LinkedHashMap<>();
            protocols.forEach(protocol -> {
                if (byName.putIfAbsent(protocol.name(), protocol) != null) {
                    throw new IllegalArgumentException("Two socket protocols are named " + protocol.name());
                }
            });
            this.protocols = Map.copyOf(byName);
            this.authenticator = authenticator;
        }

        @Override
        public void modifyHandshake(ServerEndpointConfig configuration, HandshakeRequest request, HandshakeResponse response) {
            // ⚠️ Tomcat hands this a configuration copied per session, so the principal it records belongs to that socket alone.
            List<String> offered = request.getHeaders().getOrDefault("Sec-WebSocket-Protocol", List.of());
            tokenOf(offered).flatMap(authenticator::authenticate)
                    .ifPresent(principal -> configuration.getUserProperties().put(PRINCIPAL, principal));
        }

        @Override
        public String getNegotiatedSubprotocol(List<String> supported, List<String> requested) {
            return requested.stream().filter(protocols::containsKey).findFirst().orElse("");
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getEndpointInstance(Class<T> type) {
            return (T) new ProtocolSocket(protocols);
        }
    }
}
