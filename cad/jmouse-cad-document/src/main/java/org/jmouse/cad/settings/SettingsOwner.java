package org.jmouse.cad.settings;

import java.util.Locale;

/**
 * Who owns a section of the engine's settings (ADR 0003). The engine's tree holds the first two; looks, editor feel and a reader's
 * habits are the interface's, kept in its own tree of the same subjects.
 */
public enum SettingsOwner {

    /** What is made, checked or computed — physics, standards, artwork, layout —: changed by a release, served read-only. */
    ENGINE_DATA,

    /** How an engine algorithm works — limits, passes, steps —: changed by a release, served to what runs it. */
    ENGINE_SETTINGS;

    /** How it is written where it is served: {@code engine-data}. */
    public String word() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
