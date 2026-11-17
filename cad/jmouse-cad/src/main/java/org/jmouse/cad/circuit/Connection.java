package org.jmouse.cad.circuit;

import org.jspecify.annotations.Nullable;
import java.util.Optional;

/**
 * One connection of a part: the pin it names — none when the part is connected in pin order — and the net.
 *
 * @param pin the pin it names; null when the part is connected in pin order
 * @param net the net the pin is on
 */
public record Connection(@Nullable String pin, String net) {

    public Optional<String> named() {
        return Optional.ofNullable(pin);
    }
}
