package org.jmouse.cad.circuit;

import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * What a part is tried among: a bench's parts, how long it runs, at what temperature, and whether it is realistic — its
 * instruments load what they read, a part fails as its component says.
 *
 * @param name        a bench shared by name — {@code i2c-bus} —, or null
 * @param duration    seconds it runs, or null where its expectation's measure says
 * @param temperature the temperature it runs at, kelvin, or null for the nominal bench
 * @param realistic   whether its instruments load what they read and a part fails as its component says
 * @param parts       the parts it adds round the part tried: sources, loads, instruments
 */
public record Bench(@Nullable String name, @Nullable Double duration, @Nullable Double temperature, boolean realistic, List<Part> parts) {

    public Bench {
        parts = List.copyOf(parts);
    }

    public Optional<String> shared() {
        return Optional.ofNullable(name);
    }
}
