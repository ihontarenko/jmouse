package org.jmouse.websocket;

/**
 * 🧭 One kind of conversation a socket endpoint holds, named by the subprotocol a reader asks for at the handshake —
 * {@code cad.live}, {@code cad.pcb} —: one endpoint, one path, as many protocols as are registered on it.
 */
public interface SocketProtocol {

    /** The subprotocol's name: a token, without commas or spaces. */
    String name();

    /** The longest text message a reader may send, in characters — the container's default is a few KiB. */
    int maximumMessageCharacters();

    /** A conversation for a socket just opened by this actor, its messages going out through this output. */
    SocketConversation open(SocketPrincipal principal, SocketOutput output);
}
