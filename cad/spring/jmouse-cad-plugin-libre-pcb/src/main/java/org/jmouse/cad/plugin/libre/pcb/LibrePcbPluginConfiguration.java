package org.jmouse.cad.plugin.libre.pcb;

import org.jmouse.cad.libre.pcb.LibrePcbFiling;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/** 🧩 The LibrePCB plugin, on the classpath: the project import, filed as the table ships it. */
@AutoConfiguration
public class LibrePcbPluginConfiguration {

    @Bean
    public LibrePcbPlugin cadLibrePcbPlugin() {
        return new LibrePcbPlugin(new LibrePcbDesignReading(LibrePcbFiling.standard()));
    }
}
