package org.jmouse.cad.plugin.gerber;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/** 🧩 The Gerber plugin, on the classpath: the set as it ships, and the plugin that brings it. */
@AutoConfiguration
public class GerberPluginConfiguration {

    /** The set every board house plugin writes its own on. */
    @Bean
    public GerberSet cadGerberSet() {
        return GerberSet.shipped();
    }

    @Bean
    public GerberPlugin cadGerberPlugin(GerberSet set) {
        return new GerberPlugin(set);
    }
}
