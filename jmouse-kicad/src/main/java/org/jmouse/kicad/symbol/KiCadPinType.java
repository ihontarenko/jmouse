package org.jmouse.kicad.symbol;

import java.util.Arrays;

/** A pin's electrical type, as KiCad writes it and as its electrical rules check reads it. */
public enum KiCadPinType {

    INPUT("input"),
    OUTPUT("output"),
    BIDIRECTIONAL("bidirectional"),
    TRI_STATE("tri_state"),
    PASSIVE("passive"),
    FREE("free"),
    UNSPECIFIED("unspecified"),
    POWER_INPUT("power_in"),
    POWER_OUTPUT("power_out"),
    OPEN_COLLECTOR("open_collector"),
    OPEN_EMITTER("open_emitter"),
    NOT_CONNECTED("no_connect");

    private final String word;

    KiCadPinType(String word) {
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
    public static KiCadPinType of(String word) {
        return Arrays.stream(values()).filter(type -> type.word.equals(word)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("'" + word + "' is no pin type KiCad writes: "
                        + Arrays.stream(values()).map(KiCadPinType::word).toList()));
    }
}
