package org.jmouse.grabber;

import java.time.Instant;

/**
 * 💥 A visit that could not be completed, and why.
 *
 * <p>Carried whole rather than reduced to a message, because the address alone rarely explains
 * anything: the origin says which page linked to it, the attempt count says whether the retries were
 * used up, and the route says which handler was looking at it.</p>
 *
 * @param visit     the visit that failed
 * @param route     the route handling it, or {@code null} if none claimed it
 * @param cause     what went wrong
 * @param exhausted whether the retries are used up, so this is final
 * @param at        when
 */
public record Failure(Visit visit, String route, Throwable cause, boolean exhausted, Instant at) {

    public static Failure of(Visit visit, String route, Throwable cause, boolean exhausted) {
        return new Failure(visit, route, cause, exhausted, Instant.now());
    }

    public String message() {
        return cause == null ? "unknown" : String.valueOf(cause.getMessage());
    }

    @Override
    public String toString() {
        return (exhausted ? "failed " : "failing ") + visit.address()
                + (route == null ? "" : " on " + route) + ": " + message();
    }

}
