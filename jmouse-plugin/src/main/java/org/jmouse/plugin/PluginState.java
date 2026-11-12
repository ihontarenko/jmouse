package org.jmouse.plugin;

import java.util.Set;

/**
 * Where a plugin stands: started, or refused with the reason — so a plugin left off is seen and explained, never
 * silently missing.
 */
public record PluginState(PluginDescriptor plugin, Status status, String reason, Set<String> extending) {

    public PluginState {
        extending = Set.copyOf(extending == null ? Set.of() : extending);
    }

    public enum Status {
        STARTED,
        REFUSED
    }

    /** @param extending the started plugins whose points it adds to, besides those it requires */
    static PluginState started(PluginDescriptor plugin, Set<String> extending) {
        return new PluginState(plugin, Status.STARTED, null, extending);
    }

    static PluginState refused(PluginDescriptor plugin, String reason) {
        return new PluginState(plugin, Status.REFUSED, reason, Set.of());
    }
}
