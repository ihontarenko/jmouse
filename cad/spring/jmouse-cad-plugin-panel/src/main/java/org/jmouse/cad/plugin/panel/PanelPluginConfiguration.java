package org.jmouse.cad.plugin.panel;

import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.management.CadAccessRules;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.panel.PanelFlattening;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;


/** 🧩 The panels plugin, on the classpath: its defaults, shipped as data and set per installation in the engine's settings. */
@AutoConfiguration
public class PanelPluginConfiguration {

    @Bean
    public PanelPlugin cadPanelPlugin(CadSettings sections, CadStandards standards) {
        return new PanelPlugin(new PanelFlattening(() -> sections.read(PanelPlugin.DEFAULTS), standards));
    }

    @Bean
    public PanelController cadPanelController(CadLanguage language) {
        CadAccessRules.requireDecidedInside(PanelController.class);
        return new PanelController(language);
    }
}
