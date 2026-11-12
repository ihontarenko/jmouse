package org.jmouse.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * A plugin's icon: an SVG document, and how a page paints it.
 *
 * @param svg      the document
 * @param painting a glyph the page draws in its own colour, or a maker's mark that keeps its own
 */
public record PluginIcon(String svg, Painting painting) {

    /** How an icon is painted. */
    public enum Painting {
        /** A drawing of what the plugin does, in the current colour, as the page's own icons are. */
        GLYPH,
        /** The mark of whoever the plugin speaks for — KiCad, a board house —, in the colours its owner gives it. */
        MARK
    }

    public PluginIcon {
        if (svg == null || svg.isBlank() || painting == null) {
            throw new IllegalArgumentException("An icon is an SVG document and how it is painted.");
        }
    }

    /** An icon read from a resource beside a class: {@code spice.svg}. */
    static PluginIcon read(Class<?> beside, String resource, Painting painting, String pluginId) {
        try (InputStream input = beside.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalArgumentException("Plugin '" + pluginId + "' names the icon " + resource + ", which it does not hold.");
            }
            return new PluginIcon(new String(input.readAllBytes(), StandardCharsets.UTF_8), painting);
        } catch (IOException unreadable) {
            throw new UncheckedIOException("Plugin '" + pluginId + "' could not read its icon " + resource, unreadable);
        }
    }
}
