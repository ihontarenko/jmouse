package org.jmouse.cad.pcb.routing;

import java.util.Locale;

/** How a track leaves a pad: through the pad's best breakout ({@link PadBreakouts}), or as the way to it was found. */
public enum PadExit {

    /** Out along the pad's own axes — any of eight ways out of a round pad —, then on at the reader's angles: KiCad's smart pads. */
    SMART,
    /** As the way was found: straight from where the line stands on the pad, at whatever slant. */
    AS_LAID;

    /** How it is written: {@code as-laid}. */
    public String word() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /** The one a word names; refused for a word that names none. */
    public static PadExit of(String word) {
        return valueOf(word.strip().toUpperCase(Locale.ROOT).replace('-', '_'));
    }
}
