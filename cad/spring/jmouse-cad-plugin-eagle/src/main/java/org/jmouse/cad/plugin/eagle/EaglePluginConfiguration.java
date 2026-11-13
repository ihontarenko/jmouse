package org.jmouse.cad.plugin.eagle;

import org.jmouse.cad.table.TableSource;
import org.jmouse.cad.eagle.EagleLibraries;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/** 🧩 The Eagle plugin, on the classpath: the schematic import, filed as the Eagle table ships it. */
@AutoConfiguration
public class EaglePluginConfiguration {

    @Bean
    public EaglePlugin cadEaglePlugin(TableSource tables) {
        return new EaglePlugin(new EagleDesignReading(EagleLibraries.readFrom(tables)));
    }
}
