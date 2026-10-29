package org.jmouse.cad.live;

import java.util.function.Supplier;

/**
 * Who a live socket runs for. A socket's messages arrive on threads no request filter ran on, so each one is run as
 * this actor — and decided by the host's {@code CadAuthority} exactly as a request of theirs would be.
 */
public interface LivePrincipal {

    /** The account (or other actor) the socket was opened by. */
    String actorId();

    /** Runs work as this actor, restoring whoever ran on the thread before. */
    <T> T runAs(Supplier<T> work);
}
