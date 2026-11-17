package org.jmouse.websocket;

import java.util.function.Supplier;

/**
 * 👤 Who a socket is open for. A socket's messages arrive on threads no request filter ran on, so each one is run as this
 * actor — and decided by the host's own rules exactly as a request of theirs would be.
 */
public interface SocketPrincipal {

    /** The account (or other actor) the socket was opened by. */
    String actorId();

    /** Runs work as this actor, restoring whoever ran on the thread before. */
    <T> T runAs(Supplier<T> work);
}
