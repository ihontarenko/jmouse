package org.jmouse.parallax.store.spice;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * How SPICE — the second engine — is reached: the command that starts ngspice in batch mode reading its deck from
 * standard input, word by word, and how long one run may take. Without a command there is no second engine, and the
 * routes that need one say so.
 *
 * <pre>{@code
 * jmouse.parallax.spice.command: docker, run, --rm, -i, parallax-ngspice
 * }</pre>
 */
@ConfigurationProperties("jmouse.parallax.spice")
public record SpiceProperties(List<String> command, Duration timeout) {

    public SpiceProperties {
        command = command == null ? List.of() : List.copyOf(command);
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            timeout = Duration.ofSeconds(60);
        }
    }
}
