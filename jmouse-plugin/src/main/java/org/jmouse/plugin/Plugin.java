package org.jmouse.plugin;

/**
 * A plugin: who it is, what it brings, and — when it holds something running — how it starts and stops. Everything it
 * brings is said in {@link #register}, once, before any plugin starts.
 */
public interface Plugin {

    PluginDescriptor descriptor();

    void register(PluginRegistration registration);

    /** Starts what it runs, once every plugin is registered; what the others brought is readable here. */
    default void start(Extensions extensions) {
    }

    default void stop() {
    }
}
