package org.jmouse.cad.panel;

import java.util.Arrays;

/** How the boards of a panel come apart after they are made. */
public enum PanelSeparation {

    /** Routed apart by the factory's own means: nothing is drawn for it. */
    NONE("none"),

    /** Cut along straight lines right across the panel: the boards touch, and every line must miss every board. */
    V_SCORE("v-score"),

    /** Routed round, held by tabs that break along a row of small holes. */
    MOUSE_BITES("mouse-bites");

    private final String word;

    PanelSeparation(String word) {
        this.word = word;
    }

    public String word() {
        return word;
    }

    static String[] words() {
        return Arrays.stream(values()).map(PanelSeparation::word).toArray(String[]::new);
    }

    public static PanelSeparation ofWord(String word) {
        return Arrays.stream(values()).filter(kind -> kind.word.equals(word)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("A panel is not separated by '" + word + "'"));
    }
}
