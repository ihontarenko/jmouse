package org.jmouse.cad.settings;

import org.jmouse.plugin.Contributions;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 🎚️ One section of the engine's settings tree ({@code ADR 0004}): where it stands in the tree by subject — {@code pcb/routing},
 * {@code pcb/physics/copper}, {@code plugin/gerber/fabrication} —, what it is called, who owns its values (ADR 0003), and the record
 * its file is read into. Declared once, beside its record, by the module that computes with it; a plugin declares its own and
 * contributes them to {@link #POINT}, as the engine contributes the core's.
 *
 * <p>Its file is always {@code org/jmouse/cad/settings/<path>.json} in the module that declares it, its JSON Schema beside it as
 * {@code <path>.schema.json} — what each value means, its unit, its source —, so every module's settings show as one tree.</p>
 *
 * @param path  where it stands in the tree: words of lower-case letters, digits and hyphens, joined by {@code /}
 * @param title what a reader calls it
 * @param owner who owns its values
 * @param type  the record its file is read into — an array of one for a file that is a list
 */
public record SettingsSection<T>(String path, String title, SettingsOwner owner, Class<T> type) {

    /** Every section of the tree: the core's, contributed by the engine, and each plugin's. */
    @SuppressWarnings("rawtypes")
    public static final Contributions<SettingsSection> POINT = new Contributions<>("cad.settings.sections", SettingsSection.class);

    /** The package every module's settings ship under, as a resource path. */
    public static final String ROOT = "/org/jmouse/cad/settings/";

    private static final Pattern PATH = Pattern.compile("[a-z0-9-]+(/[a-z0-9-]+)*");

    public SettingsSection {
        Objects.requireNonNull(path, "a section has a path");
        if (!PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("A settings path is lower-case words joined by '/': " + path);
        }
        Objects.requireNonNull(title, "a section has a title");
        Objects.requireNonNull(owner, "a section has an owner");
        Objects.requireNonNull(type, "a section is read into a record");
    }

    /** Its file, as a resource path. */
    public String resource() {
        return ROOT + path + ".json";
    }

    /** Its JSON Schema, as a resource path. */
    public String schema() {
        return ROOT + path + ".schema.json";
    }
}
