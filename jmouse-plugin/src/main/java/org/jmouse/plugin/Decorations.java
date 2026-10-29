package org.jmouse.plugin;

/**
 * A point that wraps one service of its owner: every decorator in turn, the lowest order nearest the owner's own
 * service. Its owner declares it only for a service meant to be wrapped — a decoration is a promise of the owner's.
 */
public record Decorations<T>(String name, Class<T> type) implements ExtensionPoint<T> {

    public Decorations {
        ExtensionPoint.requireNamed(name, type);
    }
}
