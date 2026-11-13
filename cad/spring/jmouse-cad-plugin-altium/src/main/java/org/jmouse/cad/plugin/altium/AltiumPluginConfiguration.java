package org.jmouse.cad.plugin.altium;

import org.jmouse.cad.table.TableSource;
import org.jmouse.cad.altium.AltiumLibraries;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/** 🧩 The Altium plugin, on the classpath: the import of a schematic and of a PCB library, filed as the Altium table ships it. */
@AutoConfiguration
public class AltiumPluginConfiguration {

    @Bean
    public AltiumPlugin cadAltiumPlugin(TableSource tables) {
        return new AltiumPlugin(new AltiumReading(AltiumLibraries.readFrom(tables)));
    }
}
