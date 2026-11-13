package org.jmouse.cad.settings;

import org.jmouse.settings.ParameterTree;
import org.jmouse.settings.Section;
import org.jmouse.settings.Settings;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🎚️ The engine's settings tree as a module computes with it (ADR 0004, 0007): a section's values, read each time they are used,
 * so a change applies at once. A value is read here and nowhere else: a module that computes with a section asks for it, never for
 * its file or its rows.
 *
 * <p>Two sources, and only two: {@link #kept(Settings)} — the installation's rows, the one source a running engine reads —, and
 * {@link #shipped()} — the files the modules ship, each a section's seed, for whatever computes without an installation: a test,
 * a schema's check.</p>
 */
public interface CadSettings {

    /** A section's values as they stand now. */
    <T> T read(SettingsSection<T> section);

    /** The installation's values: each section a section of its plugin's settings, its values rows. */
    static CadSettings kept(Settings settings) {
        return new CadSettings() {
            @Override
            public <T> T read(SettingsSection<T> section) {
                return settings.read(section.plugin(), section.pathInOwner(), section.type());
            }
        };
    }

    /** The files the modules ship, each read once a process — a file that is not its record refused, naming the section. */
    static CadSettings shipped() {
        return Shipped.SHIPPED;
    }

    /** A section as its plugin declares it: its file, unfolded into the rows the installation is born with. */
    static Section declarationOf(SettingsSection<?> section) {
        return Section.structured(section.pathInOwner(), section.title(), null, section.type(),
                                  ParameterTree.unfold(CadSectionReader.JSON.treeOf(section.shippedText())));
    }

    /** The shipped files, read once each. */
    final class Shipped implements CadSettings {

        private static final Shipped SHIPPED = new Shipped();

        private final Map<String, Object> read = new ConcurrentHashMap<>();

        private Shipped() {
        }

        @Override
        public <T> T read(SettingsSection<T> section) {
            return section.type().cast(read.computeIfAbsent(section.path(), path -> {
                try {
                    return CadSectionReader.JSON.read(CadSectionReader.JSON.treeOf(section.shippedText()), section.type());
                } catch (IllegalArgumentException refused) {
                    throw new IllegalStateException("The settings " + section.path() + " are not " + section.type().getSimpleName()
                                                    + ": " + refused.getMessage(), refused);
                }
            }));
        }
    }
}
