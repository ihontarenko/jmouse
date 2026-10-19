package org.jmouse.parallax.component;

/**
 * The physical constants the engine itself needs, once — the same numbers the catalogue's shared {@code constants}
 * document gives every component ({@code catalogue/electronics/functions/constants.jmx}): a model writes
 * {@code boltzmann}, never its digits.
 */
public final class PhysicalConstants {

    /** Boltzmann's constant, J/K (exact since 2019). */
    public static final double BOLTZMANN = 1.380649e-23;

    /** The elementary charge, C (exact since 2019). */
    public static final double ELEMENTARY_CHARGE = 1.602176634e-19;

    /** 0 °C in kelvin. */
    public static final double ZERO_CELSIUS = 273.15;

    private PhysicalConstants() {
    }

    /** A temperature in °C, in kelvin. */
    public static double kelvin(double celsius) {
        return celsius + ZERO_CELSIUS;
    }

    /** A temperature in kelvin, in °C. */
    public static double celsius(double kelvin) {
        return kelvin - ZERO_CELSIUS;
    }

    /** The thermal voltage kT/q at a temperature in kelvin. */
    public static double thermalVoltage(double kelvin) {
        return BOLTZMANN * kelvin / ELEMENTARY_CHARGE;
    }
}
