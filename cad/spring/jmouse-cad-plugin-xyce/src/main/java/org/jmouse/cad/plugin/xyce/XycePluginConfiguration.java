package org.jmouse.cad.plugin.xyce;

import org.jmouse.cad.spice.SpiceDevices;
import org.jmouse.cad.table.TableSource;
import org.jmouse.cad.spice.SpiceProcess;
import org.jmouse.settings.ParameterValues;
import org.jmouse.settings.Settings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.util.List;

/** 🧩 The Xyce plugin, on the classpath: its plugin bean, Xyce as the server names it. */
@AutoConfiguration
@EnableConfigurationProperties(XycePluginConfiguration.XyceProperties.class)
public class XycePluginConfiguration {

    /**
     * {@code jmouse.cad.xyce.*}: how this server reaches Xyce.
     *
     * @param command the program that runs a deck read from its standard input and prints the run's CSV on its output,
     *                word by word — {@code docker run --rm -i jm_cad-xyce} in a container —; empty for a server without it,
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
    public XycePlugin cadXycePlugin(XyceProperties properties, Settings settings, TableSource tables) {
        ParameterValues values = settings.parameters(XycePlugin.ID, SpiceProcess.PROGRAM);
        SpiceProcess simulator = new SpiceProcess("Xyce", () -> values.command(SpiceProcess.COMMAND), () -> values.duration(SpiceProcess.TIMEOUT));
        return new XycePlugin(simulator, properties.command(), properties.timeout(), SpiceDevices.readFrom(tables));
    }
}
