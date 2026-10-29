package org.jmouse.cad.circuit;

import java.util.List;
import java.util.Optional;

/**
 * What a part is tried among: a bench's parts, how long it runs, at what temperature, and whether it is realistic — its
 * instruments load what they read, a part fails as its component says.
 *
 * @param name        a bench shared by name — {@code i2c-bus} —, or null
 * @param duration    seconds it runs, or null where its expectation's measure says
 * @param temperature the temperature it runs at, kelvin, or null for the nominal bench
 */
public record Bench(String name, Double duration, Double temperature, boolean realistic, List<Part> parts) {

    public Bench {
        parts = List.copyOf(parts);
    }

    public Optional<String> shared() {
        return Optional.ofNullable(name);
    }
}
