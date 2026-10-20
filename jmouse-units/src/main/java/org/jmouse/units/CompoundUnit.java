package org.jmouse.units;

import java.util.Optional;

/**
 * A written unit read whole — {@code kOhm}, {@code V/us}, {@code A/V^2}, {@code 1/s}: the factors' prefixes and scales
 * multiplied together, the base units they are kept in, and what the whole measures.
 *
 * @param factor    what one of it is in its base units: {@code 1e6} for {@code V/us}
 * @param base      the factors without their prefixes: {@code V/s} for {@code V/us}
 * @param dimension what it measures; empty when one of its factors names no physical quantity
 */
public record CompoundUnit(double factor, String base, Optional<Dimension> dimension) {
}
