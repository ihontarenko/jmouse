package org.jmouse.websocket;

import java.util.Optional;

/**
 * 🔑 The host's answer to who opened a socket. The token is read from the handshake's {@code bearer.<token>} subprotocol and
 * asked about here once; a socket nobody is found for is closed at once.
 */
@FunctionalInterface
public interface SocketAuthenticator {

    /** The actor the token belongs to; empty for a token that is unknown, expired or of a disabled actor. */
    Optional<? extends SocketPrincipal> authenticate(String token);
}
