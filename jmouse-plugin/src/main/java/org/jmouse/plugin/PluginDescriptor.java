package org.jmouse.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Who a plugin is, as an application lists it to a reader.
 *
 * @param description what it brings, in a sentence; null when it says nothing
 * @param author      who made it; null when it does not say
 * @param icon        its icon as an SVG document, drawn in the current colour; null for none
 * @param requires    the plugins it stands on that its points do not say: a plugin contributing to another's point
 *                    stands on it already
 */
public record PluginDescriptor(String id, String title, String version, String description, String author, String icon,
                               Set<String> requires) {

    public PluginDescriptor {
        // One word, so it is one key of a setting: jmouse.plugins.cad-spice.enabled.
        if (id == null || !id.matches("[a-z0-9]+(-[a-z0-9]+)*")) {
            throw new IllegalArgumentException("A plugin id is one word of lowercase letters, digits and dashes, as "
                                               + "cad-spice, not '" + id + "'.");
        }
        title = title == null || title.isBlank() ? id : title;
        requires = Set.copyOf(requires == null ? Set.of() : requires);
    }

    /**
     * A plugin described by the jar that holds it: its version, its maker and what it brings, as the jar's manifest says
     * them — never a version written in its code.
     */
    public static PluginDescriptor ofArtifact(String id, String title, Class<?> plugin) {
        ArtifactFacts facts = ArtifactFacts.of(plugin);
        return new PluginDescriptor(id, title, facts.version(), facts.description(), facts.author(), null, Set.of());
    }

    /** A plugin that says only who it is. */
    public static PluginDescriptor of(String id, String title, String version) {
        return new PluginDescriptor(id, title, version, null, null, null, Set.of());
    }

    /** This descriptor, saying what the plugin brings and who made it. */
    public PluginDescriptor describedAs(String description, String author) {
        return new PluginDescriptor(id, title, version, description, author, icon, requires);
    }

    /** This descriptor with its icon, read from a resource beside the plugin's class: {@code spice.svg}. */
    public PluginDescriptor drawnBy(Class<?> plugin, String resource) {
        try (InputStream input = plugin.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalArgumentException("Plugin '" + id + "' names the icon " + resource + ", which it does not hold.");
            }
            return new PluginDescriptor(id, title, version, description, author,
                                        new String(input.readAllBytes(), StandardCharsets.UTF_8), requires);
        } catch (IOException unreadable) {
            throw new UncheckedIOException("Plugin '" + id + "' could not read its icon " + resource, unreadable);
        }
    }

    /** This descriptor, standing on these plugins as well. */
    public PluginDescriptor requiring(Set<String> plugins) {
        return new PluginDescriptor(id, title, version, description, author, icon, plugins);
    }
}
