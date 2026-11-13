package org.jmouse.cad.plugin.pcbway;

import org.jmouse.cad.plugin.gerber.GerberSet;
import org.jmouse.cad.settings.CadSettings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/** 🧩 The PCBWay plugin, on the classpath: its plugin bean, on the Gerber plugin's set. */
@AutoConfiguration(afterName = "org.jmouse.cad.plugin.gerber.GerberPluginConfiguration")
public class PcbwayPluginConfiguration {

    @Bean
    public PcbwayPlugin cadPcbwayPlugin(GerberSet set, CadSettings sections) {
        return new PcbwayPlugin(set, sections);
    }
}
