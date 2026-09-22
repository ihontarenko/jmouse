package org.jmouse.grabber;

import java.net.URI;
import java.util.Objects;

/**
 * 📍 One address to be visited, and everything travelling with it.
 *
 * <p>This is the unit of work: the queue holds visits, the journal remembers them, the engine
 * processes them one at a time, and a handler receives the one it is looking at through the
 * {@link PageContext}.</p>
 *
 * @param address    where to go
 * @param depth      how many follows away from a seed this is — {@code 0} for a seed itself
 * @param route      the name of the route that should claim it, or {@code null} to let the table match
 * @param attributes the baton from the page that discovered it
 * @param origin     what discovered it
 * @param attempt    which attempt this is, from {@code 1}
 */
public record Visit(
        URI address,
        int depth,
        String route,
        Attributes attributes,
        VisitOrigin origin,
        int attempt
) {

    public Visit {
        Objects.requireNonNull(address, "A visit needs an address");

        if (depth < 0) {
            throw new IllegalArgumentException("A visit depth cannot be negative, got " + depth);
        }

        if (attempt < 1) {
            throw new IllegalArgumentException("A visit attempt starts at 1, got " + attempt);
        }

        attributes = attributes == null ? Attributes.empty() : attributes;
        origin = origin == null ? VisitOrigin.seed() : origin;
    }

    /**
     * 🌱 A seed — depth zero, no route hint, nothing carried, discovered by nobody.
     */
    public static Visit seed(URI address) {
        return new Visit(address, 0, null, Attributes.empty(), VisitOrigin.seed(), 1);
    }

    /**
     * 🌱 A seed with a baton already on it, for a run whose first pages need context of their own.
     */
    public static Visit seed(URI address, Attributes attributes) {
        return new Visit(address, 0, null, attributes, VisitOrigin.seed(), 1);
    }

    /**
     * 🔗 A visit discovered on the page this one describes — one level deeper, carrying the same
     * baton unless the caller adds to it.
     */
    public Visit discover(URI discovered, String routeName, Attributes carried) {
        return new Visit(
                discovered,
                depth + 1,
                routeName,
                attributes.merge(carried),
                VisitOrigin.discoveredOn(address, route),
                1
        );
    }

    /**
     * ♻️ The same visit, one attempt later. Depth and baton are unchanged: a retry is the same work,
     * not new work.
     */
    public Visit retried() {
        return new Visit(address, depth, route, attributes, origin, attempt + 1);
    }

    /**
     * 🧭 The same visit at another address — how a normalised address replaces the one a page
     * actually wrote.
     */
    public Visit at(URI other) {
        if (address.equals(other)) {
            return this;
        }

        return new Visit(other, depth, route, attributes, origin, attempt);
    }

    /**
     * 🏷️ The same visit, sent to a named route rather than matched.
     */
    public Visit routedTo(String routeName) {
        return new Visit(address, depth, routeName, attributes, origin, attempt);
    }

    /**
     * ➕ The same visit with one more attribute on its baton.
     */
    public Visit carrying(String name, Object value) {
        return new Visit(address, depth, route, attributes.with(name, value), origin, attempt);
    }

    public boolean isSeed() {
        return depth == 0 && origin.isSeed();
    }

    public boolean isFirstAttempt() {
        return attempt == 1;
    }

    @Override
    public String toString() {
        return "Visit[" + address + " depth=" + depth
                + (route == null ? "" : " route=" + route)
                + (attempt == 1 ? "" : " attempt=" + attempt) + "]";
    }

}
