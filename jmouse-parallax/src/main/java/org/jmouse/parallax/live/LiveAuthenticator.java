package org.jmouse.parallax.live;

import java.util.Optional;

/**
 * The host's answer to who opened a live socket. The socket reads the bearer token from its {@code bearer.<token>}
 * subprotocol and asks here once, at the handshake; a socket nobody is found for is closed at once.
 */
public interface LiveAuthenticator {

    /** The actor the token belongs to; empty for a token that is unknown, expired or of a disabled actor. */
    Optional<LivePrincipal> authenticate(String token);
}
