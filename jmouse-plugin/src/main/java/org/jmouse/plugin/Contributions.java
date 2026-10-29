package org.jmouse.plugin;

/**
 * A point that holds many contributions, each standing alone: the formats a document is exchanged in, the engines an
 * analysis may run on, the entries of a menu. Its owner reads them all, in the order the plugins start.
 */
public record Contributions<T>(String name, Class<T> type) implements ExtensionPoint<T> {

    public Contributions {
        ExtensionPoint.requireNamed(name, type);
    }
}
