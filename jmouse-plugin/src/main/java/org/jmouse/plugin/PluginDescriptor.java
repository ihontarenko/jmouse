package org.jmouse.plugin;

import java.util.Set;

/**
 * Who a plugin is, as an application lists it to a reader.
 *
 * @param summary     what it brings, in one line a list shows; null when it says nothing
 * @param category    the kind of plugin it is, a list's group — {@code Formats}, {@code Fabrication} —; null for none
 * @param author      who made it; null when it does not say
 * @param icon        its icon, and how a page paints it; null for none
 * @param requires    the plugins it stands on that its points do not say: a plugin contributing to another's point
 *                    stands on it already
 */
public record PluginDescriptor(String id, String title, String version, String summary, String category, String author,
                               PluginIcon icon, Set<String> requires) {

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
     * A plugin described by the jar that holds it: its version, its maker, what it brings and its kind, as the jar's
     * manifest says them ({@link ArtifactFacts}) — never written in its code.
     */
    public static PluginDescriptor ofArtifact(String id, String title, Class<?> plugin) {
        ArtifactFacts facts = ArtifactFacts.of(plugin);
        return new PluginDescriptor(id, title, facts.version(), facts.summary(), facts.category(), facts.author(), null, Set.of());
    }

    /** A plugin that says only who it is. */
    public static PluginDescriptor of(String id, String title, String version) {
        return new PluginDescriptor(id, title, version, null, null, null, null, Set.of());
    }

    /**
     * This descriptor with a glyph of what it does — drawn in the page's colour —, read from a resource beside the
     * plugin's class: {@code spice.svg}.
     */
    public PluginDescriptor drawnBy(Class<?> plugin, String resource) {
        return iconed(PluginIcon.read(plugin, resource, PluginIcon.Painting.GLYPH, id));
    }

    /**
     * This descriptor with the mark of whoever it speaks for — KiCad's, a board house's —, kept in its own colours, read
     * from a resource beside the plugin's class.
     */
    public PluginDescriptor markedBy(Class<?> plugin, String resource) {
        return iconed(PluginIcon.read(plugin, resource, PluginIcon.Painting.MARK, id));
    }

    private PluginDescriptor iconed(PluginIcon given) {
        return new PluginDescriptor(id, title, version, summary, category, author, given, requires);
    }

    /** This descriptor, standing on these plugins as well. */
    public PluginDescriptor requiring(Set<String> plugins) {
        return new PluginDescriptor(id, title, version, summary, category, author, icon, plugins);
    }
}
