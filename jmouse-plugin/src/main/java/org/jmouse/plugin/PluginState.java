package org.jmouse.plugin;

/**
 * Where a plugin stands: started, or refused with the reason — so a plugin left off is seen and explained, never
 * silently missing.
 */
public record PluginState(PluginDescriptor plugin, Status status, String reason) {

    public enum Status {
        STARTED,
        REFUSED
    }

    static PluginState started(PluginDescriptor plugin) {
        return new PluginState(plugin, Status.STARTED, null);
    }

    static PluginState refused(PluginDescriptor plugin, String reason) {
        return new PluginState(plugin, Status.REFUSED, reason);
    }
}
