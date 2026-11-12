package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.kicad.KiCadLibraries;
import org.jmouse.plugin.PluginSettings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;
import java.util.List;

/** 🧩 The KiCad plugin, on the classpath: its plugin bean over the libraries its settings name. */
@AutoConfiguration
@EnableConfigurationProperties(KiCadPluginConfiguration.KiCadProperties.class)
public class KiCadPluginConfiguration {

    /**
     * {@code jmouse.cad.kicad.*}: what a deployment gives the plugin's settings to start from; they are changed in the
     * engine's settings, and forgetting the change brings these back.
     *
     * @param footprintFolders KiCad's installed footprint libraries, each a folder of {@code .pretty} libraries —
     *                         searched after a design's own, and for parts; empty for none
     * @param symbolFolders    KiCad's installed symbol libraries, each a folder of {@code .kicad_sym} files — searched
     *                         for parts; empty for none
     * @param modelFolders     KiCad's installed 3D model libraries, each a folder of {@code .3dshapes} libraries — a
     *                         fetched footprint's models are taken from them; empty for none
     */
    @ConfigurationProperties("jmouse.cad.kicad")
    public record KiCadProperties(List<Path> footprintFolders, List<Path> symbolFolders, List<Path> modelFolders) {

        public KiCadProperties {
            footprintFolders = footprintFolders == null ? List.of() : List.copyOf(footprintFolders);
            symbolFolders = symbolFolders == null ? List.of() : List.copyOf(symbolFolders);
            modelFolders = modelFolders == null ? List.of() : List.copyOf(modelFolders);
        }
    }

    /**
     * The installed libraries are read the first time a search asks — on a virtual thread of their own, so a search is
     * answered "indexing" meanwhile rather than kept waiting.
     */
    @Bean
    public KiCadPlugin cadKiCadPlugin(KiCadProperties properties, PluginSettings settings) {
        InstalledKiCad installed = new InstalledKiCad(settings.valuesOf(KiCadPlugin.ID), KiCadLibraries.standard(),
                command -> Thread.ofVirtual().name("kicad-libraries").start(command));
        return new KiCadPlugin(installed, properties.symbolFolders(), properties.footprintFolders(), properties.modelFolders(),
                CadSettings.read(PcbArtwork.SECTION));
    }
}
