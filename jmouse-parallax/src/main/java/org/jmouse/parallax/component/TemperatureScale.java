package org.jmouse.parallax.component;

/**
 * Celsius and kelvin, one to the other: what a bench is set in and what every equation reads. Units, not physics — the
 * engine holds no physical constant; a document takes them from the catalogue's shared {@code constants}
 * ({@code catalogue/electronics/functions/constants.jmx}).
 */
public final class TemperatureScale {

    /** 0 °C in kelvin. */
    public static final double ZERO_CELSIUS = 273.15;

    private TemperatureScale() {
    }

    /** A temperature in °C, in kelvin. */
    public static double kelvin(double celsius) {
        return celsius + ZERO_CELSIUS;
    }

    /** A temperature in kelvin, in °C. */
    public static double celsius(double kelvin) {
        return kelvin - ZERO_CELSIUS;
    }
}
