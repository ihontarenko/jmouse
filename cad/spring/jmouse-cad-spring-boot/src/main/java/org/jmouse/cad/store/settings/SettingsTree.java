package org.jmouse.cad.store.settings;

import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.plugin.Extensions;

import java.util.List;
import java.util.Optional;

/**
 * 🎚️ The engine's settings tree as a server holds it (ADR 0004): the sections the engine and every running plugin contributed, each
 * read once ({@link CadSettings}). Asked of the plugins each time, so a plugin switched on or off brings or takes its sections.
 */
public final class SettingsTree {

    private final Extensions extensions;

    public SettingsTree(Extensions extensions) {
        this.extensions = extensions;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private CadSettings catalogue() {
        return new CadSettings((List) extensions.contributions(SettingsSection.POINT));
    }

    /** Every section, by path. */
    public List<SettingsSection<?>> sections() {
        return catalogue().sections();
    }

    /** The section at a path; empty for none. */
    public Optional<SettingsSection<?>> section(String path) {
        return catalogue().section(path);
    }

    /** A section's values. */
    public Object valuesOf(SettingsSection<?> section) {
        return CadSettings.read(section);
    }

    /** Every section read, at start: a file that is not its record stops the server rather than its first reader. */
    public void readEvery() {
        catalogue().readEvery();
    }
}
