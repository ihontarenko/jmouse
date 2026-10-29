package org.jmouse.plugin;

/** A point that carries what happened to whoever listens: a document saved, a run finished. */
public record Topic<T>(String name, Class<T> type) implements ExtensionPoint<T> {

    public Topic {
        ExtensionPoint.requireNamed(name, type);
    }
}
