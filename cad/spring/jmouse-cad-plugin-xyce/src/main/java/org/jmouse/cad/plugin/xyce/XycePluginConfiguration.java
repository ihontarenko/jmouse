package org.jmouse.cad.plugin.xyce;

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

/** 🧩 The Xyce plugin, on the classpath: its plugin bean, Xyce as the server names it. */
@AutoConfiguration
@EnableConfigurationProperties(XycePluginConfiguration.XyceProperties.class)
public class XycePluginConfiguration {

    /**
     * {@code jmouse.cad.xyce.*}: how this server reaches Xyce.
     *
     * @param command the program that runs a deck read from its standard input and prints the run's CSV on its output,
     *                word by word — {@code docker run --rm -i jmcad-xyce} in a container —; empty for a server without it,
     *                whose engine is then listed as off
     * @param timeout how long one run may take before its process is killed
     */
    @ConfigurationProperties("jmouse.cad.xyce")
    public record XyceProperties(List<String> command, @DefaultValue("PT120S") Duration timeout) {

        public XyceProperties {
            command = command == null ? List.of() : List.copyOf(command);
        }
    }

    @Bean
    public XycePlugin cadXycePlugin(XyceProperties properties) {
        Optional<SpiceSimulator> simulator = properties.command().isEmpty() ? Optional.empty()
                : Optional.of(new SpiceProcess("Xyce", properties.command(), properties.timeout()));
        return new XycePlugin(simulator);
    }
}
