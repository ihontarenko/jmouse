package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.kicad.InstalledKiCadParts;
import org.jmouse.cad.kicad.KiCadLibraries;
import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;
import java.util.List;

/** 🧩 The KiCad plugin, on the classpath: its plugin bean over the libraries the server names. */
@AutoConfiguration
@EnableConfigurationProperties(KiCadPluginConfiguration.KiCadProperties.class)
public class KiCadPluginConfiguration {

    /**
     * {@code jmouse.cad.kicad.*}: what this server knows of KiCad beyond what a design brings.
     *
     * @param footprintFolders KiCad's installed footprint libraries, each a folder of {@code .pretty} libraries —
     *                         searched after a design's own, and for parts; empty for none
     * @param symbolFolders    KiCad's installed symbol libraries, each a folder of {@code .kicad_sym} files — searched
     *                         for parts; empty for none
     */
    @ConfigurationProperties("jmouse.cad.kicad")
    public record KiCadProperties(List<Path> footprintFolders, List<Path> symbolFolders) {

        public KiCadProperties {
            footprintFolders = footprintFolders == null ? List.of() : List.copyOf(footprintFolders);
            symbolFolders = symbolFolders == null ? List.of() : List.copyOf(symbolFolders);
        }
    }

    /**
     * The installed libraries are read the first time a search asks — on a virtual thread of their own, so a search is
     * answered "indexing" meanwhile rather than kept waiting.
     */
    @Bean
    public KiCadPlugin cadKiCadPlugin(KiCadProperties properties) {
        KiCadFootprintLibraries footprints = properties.footprintFolders().isEmpty() ? KiCadFootprintLibraries.NONE
                : KiCadFootprintLibraries.in(properties.footprintFolders());
        InstalledKiCadParts parts = new InstalledKiCadParts(properties.symbolFolders(), properties.footprintFolders(),
                command -> Thread.ofVirtual().name("kicad-libraries").start(command), KiCadLibraries.standard());
        return new KiCadPlugin(footprints, parts);
    }
}
