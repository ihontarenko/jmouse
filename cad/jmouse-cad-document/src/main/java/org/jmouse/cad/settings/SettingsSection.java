package org.jmouse.cad.settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 🎚️ One section of the engine's settings tree ({@code ADR 0004}): where it stands in the tree by subject — {@code pcb/routing},
 * {@code pcb/physics/copper}, {@code plugin/cad-gerber/fabrication} —, what it is called, and the record its values are read into.
 * Declared once, beside its record, by the module that computes with it, and a section of the settings of the plugin it belongs
 * to (ADR 0007): the engine's for the core's, a plugin's own for those in its branch. Its values are rows, one a value, the
 * installation's, changeable and never read-only; the file it ships is only its seed — what the installation starts from and
 * what bringing a value back returns to.
 *
 * <p>Its file is always {@code org/jmouse/cad/settings/<path>.json} in the module that declares it, so every module's settings
 * show as one tree. What each value means — its unit, its source — is its record's Javadoc: the section's JSON Schema is written
 * from the record, never kept beside the file.</p>
 *
 * @param path  where it stands in the tree: words of lower-case letters, digits and hyphens, joined by {@code /}
 * @param title what a reader calls it
 * @param type  the record its value is read into — an array of one for a value that is a list
 */
public record SettingsSection<T>(String path, String title, Class<T> type) {

    /** The engine's own plugin: the core's sections are its settings. */
    public static final String ENGINE = "cad-engine";

    /** The package every module's settings ship under, as a resource path. */
    public static final String ROOT = "/org/jmouse/cad/settings/";

    /** The branch every plugin's sections stand in, each under its id. */
    public static final String PLUGIN_BRANCH = "plugin";

    private static final Pattern PATH = Pattern.compile("[a-z0-9-]+(/[a-z0-9-]+)*");

    public SettingsSection {
        Objects.requireNonNull(path, "a section has a path");
        if (!PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("A settings path is lower-case words joined by '/': " + path);
        }
        Objects.requireNonNull(title, "a section has a title");
        Objects.requireNonNull(type, "a section is read into a record");
    }

    /**
     * A plugin's section: under the plugin's own id — {@code plugin/cad-gerber/assembly} —, so whatever lists the tree finds a
     * plugin's sections by the id it runs under, and no path repeats a name by hand.
     *
     * @param plugin the plugin's id
     * @param name   the section's name within the plugin's branch: words joined by {@code /}
     */
    public static <T> SettingsSection<T> ofPlugin(String plugin, String name, String title, Class<T> type) {
        return new SettingsSection<>(PLUGIN_BRANCH + "/" + plugin + "/" + name, title, type);
    }

    /** The section of a plugin's settings at a path among its own: where it stands in the tree. */
    public static SettingsSection<?> of(String plugin, String pathInOwner, String title, Class<?> type) {
        return new SettingsSection<>(pathOf(plugin, pathInOwner), title, type);
    }

    /**
     * Where a section of a plugin's settings stands in the tree, whatever its shape: the core's at its own path, a plugin's under its
     * id — {@code plugin/cad-gerber/files}.
     */
    public static String pathOf(String plugin, String pathInOwner) {
        return plugin.equals(ENGINE) ? pathInOwner : PLUGIN_BRANCH + "/" + plugin + "/" + pathInOwner;
    }

    /** The plugin whose section it is: the one its branch names, the engine's for the core's. */
    public String plugin() {
        return inPluginBranch() ? path.split("/")[1] : ENGINE;
    }

    /** Its path among the plugin's sections: its path within the plugin's branch, the whole path for the core's. */
    public String pathInOwner() {
        return inPluginBranch() ? path.substring(PLUGIN_BRANCH.length() + plugin().length() + 2) : path;
    }

    /** Its file, as a resource path. */
    public String resource() {
        return ROOT + path + ".json";
    }

    /**
     * The file it ships, as text: its seed.
     *
     * @throws IllegalStateException when no module ships it
     */
    public String shippedText() {
        try (InputStream stream = type.getResourceAsStream(resource())) {
            if (stream == null) {
                throw new IllegalStateException("No module ships the settings " + path + " (" + resource() + ")");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    private boolean inPluginBranch() {
        return path.startsWith(PLUGIN_BRANCH + "/");
    }
}
