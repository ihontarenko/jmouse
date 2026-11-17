package org.jmouse.cad.pcb.routing;

import java.util.Locale;

/**
 * What moving a part does to the lines its pads hold. Either way nothing is traced as it goes: the part moves, with the copper
 * standing wholly on it.
 */
public enum PartDragMode {

    /** As a layout program moves a footprint: the lines stay where they ran, and what they no longer reach is a ratsnest line. */
    DETACH,
    /**
     * When it is let go, every line it pulls or pushes is laid again as a rubber band; one that cannot be — squeezed, not clear of
     * what it now meets, joining another net — is cut back to the node it is held at, a ratsnest line from there to its pad.
     */
    RECONNECT;

    /** How it is written: {@code reconnect}. */
    public String word() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The one a word names; none said, reconnect. Refused for a word that names none. */
    public static PartDragMode of(String word) {
        return word == null || word.isBlank() ? RECONNECT : valueOf(word.strip().toUpperCase(Locale.ROOT));
    }
}
