package org.jmouse.cad.plugin.odb;

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
    public OdbPlugin cadOdbPlugin() {
        PcbOdbExport.Settings settings = CadSettings.read(OdbPlugin.EXPORT);
        PcbArtwork.Settings artwork = CadSettings.read(PcbArtwork.SECTION);
        return new OdbPlugin(new PcbOdbExport(settings, artwork, StrokeFont.newStroke(), Clock.systemUTC()));
    }
}
