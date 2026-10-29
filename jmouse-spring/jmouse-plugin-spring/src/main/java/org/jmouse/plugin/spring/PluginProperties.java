package org.jmouse.plugin.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Which plugins this application runs: every plugin it holds, unless {@code jmouse.plugins.<id>.enabled} is false.
 * A plugin switched off is never registered, so whatever stands on it is refused with the reason, not broken. An id is
 * one word — {@code cad-spice} —, never dotted: a dot would split the key.
 */
@ConfigurationProperties("jmouse")
public record PluginProperties(Map<String, Switch> plugins) {

    public PluginProperties {
        plugins = plugins == null ? Map.of() : Map.copyOf(plugins);
    }

    public record Switch(Boolean enabled) {
    }

    public boolean enabled(String id) {
        Switch configured = plugins.get(id);
        return configured == null || configured.enabled() == null || configured.enabled();
    }
}
