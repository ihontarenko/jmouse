package org.jmouse.cad.plugin.spice;

import org.jmouse.cad.spice.SpiceProcess;
import org.jmouse.cad.spice.SpiceSimulator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/** 🧩 The SPICE plugin, on the classpath: its plugin bean, ngspice as the server names it. */
@AutoConfiguration
@EnableConfigurationProperties(SpicePluginConfiguration.SpiceProperties.class)
public class SpicePluginConfiguration {

    /**
     * {@code jmouse.cad.spice.*}: how this server reaches ngspice.
     *
     * @param command the program that runs a deck read from its standard input, word by word — {@code ngspice_con -b} on
     *                this machine, {@code docker run --rm -i jmcad-ngspice} in a container —; empty for a server without
     *                it, whose engine is then listed as off while reading and writing netlists still work
     * @param timeout how long one run may take before its process is killed
     */
    @ConfigurationProperties("jmouse.cad.spice")
    public record SpiceProperties(List<String> command, @DefaultValue("PT60S") Duration timeout) {

        public SpiceProperties {
            command = command == null ? List.of() : List.copyOf(command);
        }
    }

    @Bean
    public SpicePlugin cadSpicePlugin(SpiceProperties properties) {
        Optional<SpiceSimulator> simulator = properties.command().isEmpty() ? Optional.empty()
                : Optional.of(new SpiceProcess("ngspice", properties.command(), properties.timeout()));
        return new SpicePlugin(simulator);
    }
}
