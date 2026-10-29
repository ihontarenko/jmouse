package org.jmouse.plugin;

/**
 * A named, typed place to extend. Whoever reads it declares it — the core a point of its own, a plugin one others may
 * extend in turn — and nobody else reads it: a point says nothing of the domain it belongs to.
 *
 * <p>Two points are one point when their names are; a name says whose it is ({@code cad.exchange.formats},
 * {@code explorer.viewers}), so two owners never meet on one.</p>
 *
 * @param <T> what the point holds: a contribution, the service decorated, an event
 */
public sealed interface ExtensionPoint<T> permits Contributions, Decorations, Topic {

    String name();

    Class<T> type();

    /** Refuses a point without a name or a type: a point is found by its name alone. */
    static void requireNamed(String name, Class<?> type) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("An extension point needs a name.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Extension point '" + name + "' needs the type of what it holds.");
        }
    }
}
