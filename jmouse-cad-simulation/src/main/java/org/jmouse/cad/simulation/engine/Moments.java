package org.jmouse.cad.simulation.engine;

import java.util.Locale;

/**
 * A moment of a circuit's time as a person reads it in a message: four figures and an engineering prefix — «351.0 ms»,
 * not «0.3509551253567346 s», which said the same with twelve digits nobody could use.
 */
final class Moments {

    private static final String[] PREFIXES = {"p", "n", "µ", "m", ""};

    private Moments() {
    }

    static String written(double seconds) {
        if (!Double.isFinite(seconds) || seconds == 0) {
            return String.format(Locale.ROOT, "%s s", seconds == 0 ? "0" : seconds);
        }
        int thousands = (int) Math.floor(Math.log10(Math.abs(seconds)) / 3);
        int step = Math.max(-PREFIXES.length + 1, Math.min(0, thousands));
        double scaled = seconds / Math.pow(1000, step);
        return String.format(Locale.ROOT, "%.4g %ss", scaled, PREFIXES[PREFIXES.length - 1 + step]);
    }
}
