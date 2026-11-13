package org.jmouse.cad.plugin.odb;

import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.strokefont.StrokeFont;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/** 🧩 The ODB++ plugin, on the classpath: the export as its data, the pcb's artwork ships it. */
@AutoConfiguration
public class OdbPluginConfiguration {

    @Bean
    public OdbPlugin cadOdbPlugin(CadSettings sections, CadStandards standards) {
        return new OdbPlugin(new PcbOdbExport(() -> sections.read(OdbPlugin.EXPORT), () -> sections.read(PcbArtwork.SECTION),
                StrokeFont.newStroke(), Clock.systemUTC(), standards));
    }
}
