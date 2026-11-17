package org.jmouse.cad.exchange.pcb;

import java.util.Locale;

/**
 * 🕒 What a pcb's layout asked about is, as the editor tells it — a fact of the moment, never a decision: shown as a gesture goes,
 * kept as a step of the pcb, or asked to be poured — Pour copper, or the pcb just opened. Whether its copper is poured again is the
 * engine's to say ({@link CopperRefresh}).
 */
public enum LayoutMoment {

    SHOWN, KEPT, ASKED;

    /** The moment a word names; none, the layout as kept. */
    public static LayoutMoment of(String word) {
        return word == null ? KEPT : valueOf(word.toUpperCase(Locale.ROOT));
    }
}
