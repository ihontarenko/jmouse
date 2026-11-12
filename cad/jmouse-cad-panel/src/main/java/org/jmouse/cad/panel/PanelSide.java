package org.jmouse.cad.panel;

import java.util.Arrays;
import java.util.List;

/** A side of a panel, as its drawing has it: Y grows downwards, so the top is the smallest Y. */
public enum PanelSide {

    TOP("top"),
    BOTTOM("bottom"),
    LEFT("left"),
    RIGHT("right");

    private final String word;

    PanelSide(String word) {
        this.word = word;
    }

    public String word() {
        return word;
    }

    public static List<PanelSide> ofWords(List<String> words) {
        return words.stream().map(word -> Arrays.stream(values()).filter(side -> side.word.equals(word)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("A panel has no side called '" + word + "'"))).distinct().toList();
    }
}
