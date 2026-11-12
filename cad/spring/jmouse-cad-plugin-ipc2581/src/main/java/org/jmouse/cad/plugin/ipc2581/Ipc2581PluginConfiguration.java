package org.jmouse.cad.plugin.ipc2581;

import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.exchange.pcb.PcbLayers;
import org.jmouse.strokefont.StrokeFont;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/** 🧩 The IPC-2581 plugin, on the classpath: the export as its data, the pcb's artwork and its layers ship it. */
@AutoConfiguration
public class Ipc2581PluginConfiguration {

    @Bean
    public Ipc2581Plugin cadIpc2581Plugin() {
        PcbIpc2581Export.Settings settings = CadSettings.read(Ipc2581Plugin.EXPORT);
        PcbArtwork.Settings artwork = CadSettings.read(PcbArtwork.SECTION);
        PcbLayers.Settings layers = CadSettings.read(PcbLayers.SECTION);
        return new Ipc2581Plugin(new PcbIpc2581Export(settings, artwork, layers, StrokeFont.newStroke(), Clock.systemUTC()));
    }
}
