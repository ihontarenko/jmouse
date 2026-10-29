package org.jmouse.plugin;

import java.util.List;

/**
 * What an owner reads of its points. A point nobody contributed to answers empty, an undecorated service is itself, a
 * topic nobody listens to goes nowhere: the owner works the same with no plugin at all.
 */
public interface Extensions {

    <T> List<T> contributions(Contributions<T> point);

    <T> T decorated(Decorations<T> point, T service);

    <T> void publish(Topic<T> topic, T event);

    /** Every plugin offered, started or refused, in the order they started. */
    List<PluginState> plugins();

    /** The points the started plugins declared. */
    List<ExtensionPoint<?>> points();
}
