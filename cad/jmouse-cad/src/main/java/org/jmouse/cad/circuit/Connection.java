package org.jmouse.cad.circuit;

import java.util.Optional;

/** One connection of a part: the pin it names — none when the part is connected in pin order — and the net. */
public record Connection(String pin, String net) {

    public Optional<String> named() {
        return Optional.ofNullable(pin);
    }
}
