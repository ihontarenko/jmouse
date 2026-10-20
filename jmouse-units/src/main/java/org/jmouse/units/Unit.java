package org.jmouse.units;

import java.util.Optional;

/**
 * One unit of the table, as it is read: {@code V}, {@code Ohm}, {@code degC}, {@code %}.
 *
 * @param symbol     how it is read — {@code Ohm}, not {@code Ω}
 * @param dimension  what it measures; empty for a unit that names no physical quantity ({@code div}, {@code bit})
 * @param scale      what one of it is in its base unit: {@code 0.01} for {@code %}, π/180 for {@code deg}
 * @param base       the unit its values are kept in: {@code rad} for {@code deg}, itself otherwise
 * @param prefixable whether an SI prefix may stand in front of it: {@code kOhm} is a unit, {@code k%} is not
 */
public record Unit(String symbol, Optional<Dimension> dimension, double scale, String base, boolean prefixable) {

    /** A unit an SI prefix may stand in front of, kept in itself. */
    static Unit prefixable(String symbol, Dimension dimension) {
        return new Unit(symbol, Optional.of(dimension), 1, symbol, true);
    }

    /** A unit that stands on its own. */
    static Unit standalone(String symbol, Dimension dimension, double scale, String base) {
        return new Unit(symbol, Optional.ofNullable(dimension), scale, base, false);
    }
}
