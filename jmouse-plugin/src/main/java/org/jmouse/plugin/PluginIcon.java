package org.jmouse.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * A plugin's icon: an SVG document in its own colours, drawn to the edges of its square — the mark of whoever the plugin speaks
 * for (KiCad's, a board house's), or a tile of what it does —, so it reads the same on a light page and a dark one, at the size a
 * list gives it, with nothing laid under it.
 *
 * @param svg the document
 */
public record PluginIcon(String svg) {

    public PluginIcon {
        if (svg == null || svg.isBlank()) {
            throw new IllegalArgumentException("An icon is an SVG document.");
        }
    }

    /** An icon read from a resource beside a class: {@code spice.svg}. */
    static PluginIcon read(Class<?> beside, String resource, String pluginId) {
        try (InputStream input = beside.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalArgumentException("Plugin '" + pluginId + "' names the icon " + resource + ", which it does not hold.");
            }
            return new PluginIcon(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException unreadable) {
            throw new UncheckedIOException("Plugin '" + pluginId + "' could not read its icon " + resource, unreadable);
        }
    }
}
