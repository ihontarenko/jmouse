package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.library.CadStandards;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.jmouse.cad.settings.CadSettings;
import org.springframework.context.annotation.Bean;

/** 🧩 The Gerber plugin, on the classpath: the set as it ships, and the plugin that brings it. */
@AutoConfiguration
public class GerberPluginConfiguration {

    /** The set every pcb house plugin writes its own on. */
    @Bean
    public GerberSet cadGerberSet(CadSettings sections, CadStandards standards) {
        return new GerberSet(sections, standards);
    }

    @Bean
    public GerberPlugin cadGerberPlugin(GerberSet set, CadSettings sections) {
        return new GerberPlugin(set, sections);
    }
}
