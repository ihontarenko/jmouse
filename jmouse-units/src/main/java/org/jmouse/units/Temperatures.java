package org.jmouse.units;

/**
 * The two scales an absolute temperature is written in — kelvin, and degrees Celsius counted from where water freezes —
 * and the one step between them. The units table reads °C as a step of temperature, the size of a kelvin, so a drift per
 * °C is per kelvin; an absolute temperature crosses scales only here.
 */
public final class Temperatures {

    /** Where water freezes, kelvin: 0 °C. */
    public static final double ZERO_CELSIUS = 273.15;

    private Temperatures() {
    }

    public static double kelvin(double celsius) {
        return celsius + ZERO_CELSIUS;
    }

    public static double celsius(double kelvin) {
        return kelvin - ZERO_CELSIUS;
    }
}
