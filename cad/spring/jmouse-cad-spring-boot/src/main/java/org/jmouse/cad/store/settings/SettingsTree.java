package org.jmouse.cad.store.settings;

import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.plugin.Extensions;
import org.jmouse.plugin.PluginState;
import org.jmouse.settings.Section;
import org.jmouse.settings.Settings;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🎚️ The engine's settings tree as a server holds it (ADR 0004, 0007): every structured section the engine and each running plugin
 * declares, its values the installation's rows. Asked of the plugins each time, so a plugin switched on or off brings or takes its
 * sections; a section is changed as any section of its plugin's settings is.
 */
public final class SettingsTree {

    private final Extensions  extensions;
    private final Settings    settings;
    private final CadSettings values;

    public SettingsTree(Extensions extensions, Settings settings, CadSettings values) {
        this.extensions = extensions;
        this.settings = settings;
        this.values = values;
    }

    /** Every structured section, by path. */
    public List<SettingsSection<?>> sections() {
        return extensions.plugins().stream()
                .filter(state -> state.status() == PluginState.Status.STARTED)
                .flatMap(state -> settings.sectionsOf(state.plugin().id()).stream()
                        .filter(section -> section.shape() instanceof Section.Structured)
                        .<SettingsSection<?>>map(section -> SettingsSection.of(state.plugin().id(), section.path(), section.label(),
                                                                              ((Section.Structured) section.shape()).type())))
                .sorted(Comparator.comparing(SettingsSection::path))
                .toList();
    }

    /** The section at a path; empty for none. */
    public Optional<SettingsSection<?>> section(String path) {
        return sections().stream().filter(section -> section.path().equals(path)).findFirst();
    }

    /** A section's values, as the installation keeps them now. */
    public Object valuesOf(SettingsSection<?> section) {
        return values.read(section);
    }

    /** A section's rows as the installation keeps them, one a value: what an editor changes key by key. */
    public Map<String, String> parametersOf(SettingsSection<?> section) {
        return settings.valuesOf(section.plugin(), section.pathInOwner());
    }

    /** What a section's module ships, as its record reads it: what a value goes back to. */
    public Object shippedOf(SettingsSection<?> section) {
        return CadSettings.shipped().read(section);
    }

    /** Every section read, at start: a kept row that does not fold into its record stops the server rather than its first reader. */
    public void readEvery() {
        sections().forEach(values::read);
    }
}
