package org.jmouse.plugin;

import java.util.function.Consumer;

/**
 * The one way a plugin reaches anything. No method names a kind of extension: what a plugin brings is said by the point
 * it brings it to.
 */
public interface PluginRegistration {

    /** Opens a point for others: they contribute to it, decorate it or listen to it, and this plugin reads it. */
    <T> void declare(ExtensionPoint<T> point);

    <T> void contribute(Contributions<T> point, T contribution);

    <T> void decorate(Decorations<T> point, Decorator<T> decorator);

    <T> void subscribe(Topic<T> topic, Consumer<? super T> listener);
}
