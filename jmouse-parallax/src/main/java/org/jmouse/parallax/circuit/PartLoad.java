package org.jmouse.parallax.circuit;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * How hard a part of a running circuit works, over the samples just computed.
 *
 * @param power       the average power it took from the circuit, in watts; negative for a source that delivered it
 * @param terminals   the current into each of its terminals at the last sample, in amperes, by terminal: "+" and "−"
 *                    for a two-terminal part, the pin names of a model
 * @param temperature how warm it is now, in °C, as its component's {@code heat} law makes it ({@link PartHeat}); NaN
 *                    for a part that does not warm (a source, a capacitor)
 * @param maximum     the temperature its component lets it reach, °C; NaN when it sets none
 * @param rating      the power its component rates it for, W; NaN when it states none
 * @param failure     how it failed — {@code open} or {@code short} — on a realistic bench; null while it works
 * @param displays    what its symbol shows now, by indicator name ({@link PartDisplays}); empty for a part that shows nothing
 * @param moments     the display figures that are values at a moment — a character, a count —, taken as they stand and
 *                    never averaged with another frame's; the rest are signals, averaged
 * @param pictures    the pictures it shows — a display's framebuffer —, each its bytes in hexadecimal, by figure
 * @param cause       why it failed: {@code overheated}, or — when its component displays {@code overvoltage} or
 *                    {@code overcurrent}, as a percentage of its limit — the one that reached 100 %; null while it works
 */
public record PartLoad(double power, Map<String, Double> terminals, double temperature, double maximum, double rating,
                       String failure, Map<String, Double> displays, Set<String> moments, Map<String, String> pictures,
                       String cause) {

    /** The display figures a component writes its limits as, a percentage of each; at 100 % a realistic bench fails it. */
    public static final List<String> STRESSES = List.of("overvoltage", "overcurrent");

    public PartLoad {
        terminals = terminals == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(terminals));
        displays = displays == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(displays));
        moments = moments == null ? Set.of() : Set.copyOf(moments);
        pictures = pictures == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(pictures));
    }

    /** A part's load before anything is shown on its symbol. */
    public PartLoad(double power, Map<String, Double> terminals, double temperature, double maximum, double rating, String failure) {
        this(power, terminals, temperature, maximum, rating, failure, Map.of(), Set.of(), Map.of(), null);
    }

    /** The same load with what its symbol shows: its figures, which of them are moments, and its pictures. */
    public PartLoad showing(Map<String, Double> shown, Set<String> momentFigures, Map<String, String> shownPictures) {
        return new PartLoad(power, terminals, temperature, maximum, rating, failure, shown, momentFigures, shownPictures, cause);
    }

    /** The same load, failed for this reason. */
    public PartLoad causedBy(String reason) {
        return new PartLoad(power, terminals, temperature, maximum, rating, failure, displays, moments, pictures, reason);
    }

    /** Why it should fail now, on a realistic bench: overheated, or a limit it displays reached; null while it holds. */
    public String breaking() {
        if (overheated()) {
            return "overheated";
        }
        return STRESSES.stream().filter(stress -> displays.getOrDefault(stress, 0.0) >= 100).findFirst().orElse(null);
    }

    /**
     * Which limit it is past now, whatever the bench: {@link #breaking()}'s causes — too hot, a stress figure at 100 % —
     * or, short of those, {@code overloaded}: taking more than its rating, which on a realistic bench heats it towards
     * its limit rather than failing it at once. Null while it is within all of them. The one verdict the sheet marks.
     */
    public String past() {
        String breaking = breaking();
        if (breaking != null) {
            return breaking;
        }
        return Double.isFinite(rating) && rating > 0 && power > rating ? "overloaded" : null;
    }

    /** A part that knows no limits. */
    public PartLoad(double power, Map<String, Double> terminals, double temperature) {
        this(power, terminals, temperature, Double.NaN, Double.NaN, null);
    }

    /** Whether it is hotter than its component lets it be. */
    public boolean overheated() {
        return Double.isFinite(maximum) && temperature > maximum;
    }
}
