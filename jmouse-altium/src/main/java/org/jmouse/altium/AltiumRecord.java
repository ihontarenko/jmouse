package org.jmouse.altium;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * One record of an Altium file, as it writes it: {@code |RECORD=2|OWNERINDEX=7|LOCATION.X=300|…}. Keys are read in upper
 * case — the binary file writes {@code Location.X} where the ASCII one writes {@code LOCATION.X} —, and a key Altium also
 * writes in UTF-8 ({@code %UTF8%NAME}) is that value.
 *
 * @param index where it stands among the file's records, the header left out: what an {@code OWNERINDEX} names
 */
public record AltiumRecord(int index, Map<String, String> properties) {

    /** A coordinate's whole units; its fraction is {@code _FRAC}, in hundred-thousandths of a unit. */
    private static final String FRACTION = "_FRAC";
    private static final double FRACTION_UNITS = 100_000;

    public AltiumRecord {
        Map<String, String> upper = new LinkedHashMap<>();
        properties.forEach((key, value) -> upper.put(key.toUpperCase(Locale.ROOT), value));
        properties = Collections.unmodifiableMap(upper);
    }

    /** What kind of record it is: {@code RECORD}; -1 for one that does not say. */
    public int kind() {
        return integer("RECORD", -1);
    }

    public Optional<String> text(String key) {
        return Optional.ofNullable(properties.get(key.toUpperCase(Locale.ROOT)));
    }

    public String text(String key, String otherwise) {
        return text(key).orElse(otherwise);
    }

    public int integer(String key, int otherwise) {
        return text(key).map(String::strip).map(Integer::parseInt).orElse(otherwise);
    }

    /** Altium's truth: {@code T}; anything else, or nothing, is false. */
    public boolean flag(String key) {
        return text(key).map(value -> value.equalsIgnoreCase("T")).orElse(false);
    }

    /** A coordinate, in Altium's units of ten mils: its whole units and its {@code _FRAC}. */
    public double coordinate(String key) {
        return integer(key, 0) + integer(key + FRACTION, 0) / FRACTION_UNITS;
    }
}
