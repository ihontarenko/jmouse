package org.jmouse.plugin;

import org.jmouse.settings.Section;

import java.util.List;

/**
 * A plugin: who it is, what it brings, what it may be set to, and — when it holds something running — how it starts and
 * stops. Everything it brings is said in {@link #register}, once, before any plugin starts.
 */
public interface Plugin {

    PluginDescriptor descriptor();

    void register(PluginRegistration registration);

    /**
     * What the people who run it may set it to while it runs: its sections of settings (jmouse-settings), owned by its id, in the
     * order a reader is shown them; it reads their values through the application's {@code Settings}. Nothing, for a plugin with
     * no setting.
     */
    default List<Section> settings() {
        return List.of();
    }

    /** Starts what it runs, once every plugin is registered; what the others brought is readable here. */
    default void start(Extensions extensions) {
    }

    default void stop() {
    }
}
