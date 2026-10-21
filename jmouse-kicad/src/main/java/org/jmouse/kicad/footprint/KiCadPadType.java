package org.jmouse.kicad.footprint;

import java.util.Arrays;

/** What a pad is on the board, as KiCad writes it: plated through the board, on its surface, or a bare hole. */
public enum KiCadPadType {

    THROUGH_HOLE("thru_hole"),
    SURFACE_MOUNT("smd"),
    CONNECTOR("connect"),
    UNPLATED_HOLE("np_thru_hole");

    private final String word;

    KiCadPadType(String word) {
        this.word = word;
    }

    public String word() {
        return word;
    }

    /**
     * The type a word writes.
     *
     * @throws IllegalArgumentException naming the types KiCad has, for any other word
     */
    public static KiCadPadType of(String word) {
        return Arrays.stream(values()).filter(type -> type.word.equals(word)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("'" + word + "' is no pad type KiCad writes: "
                        + Arrays.stream(values()).map(KiCadPadType::word).toList()));
    }
}
