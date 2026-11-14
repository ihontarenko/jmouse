package org.jmouse.cad.plugin.libre.pcb;

import org.jmouse.cad.libre.pcb.LibrePcbFiling;
import org.jmouse.cad.table.ReadTable;
import org.jmouse.cad.table.TableSource;
import org.jmouse.settings.Settings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;
import java.util.List;

/** 🧩 The LibrePCB plugin, on the classpath: the project import, filed as the table ships it, and the libraries installed beside it. */
@AutoConfiguration
@EnableConfigurationProperties(LibrePcbPluginConfiguration.LibrePcbProperties.class)
public class LibrePcbPluginConfiguration {

    /**
     * {@code jmouse.cad.libre-pcb.*}: what a deployment gives the plugin's settings to start from; they are changed in the engine's
     * settings, and forgetting the change brings these back.
     *
     * @param libraryFolders the folders LibrePCB's library manager installs {@code *.lplib} libraries in — a workspace's
     *                       {@code data/libraries/remote} and {@code local} —, read for parts; empty for none
     */
    @ConfigurationProperties("jmouse.cad.libre-pcb")
    public record LibrePcbProperties(List<Path> libraryFolders) {

        public LibrePcbProperties {
            libraryFolders = libraryFolders == null ? List.of() : List.copyOf(libraryFolders);
        }
    }

    /** The installed libraries are read on a virtual thread of their own, so a search is answered "indexing" meanwhile. */
    @Bean
    public LibrePcbPlugin cadLibrePcbPlugin(LibrePcbProperties properties, Settings settings, TableSource tables) {
        ReadTable<LibrePcbFiling> filings = LibrePcbFiling.readFrom(tables);
        InstalledLibrePcb installed = new InstalledLibrePcb(settings.parameters(LibrePcbPlugin.ID, LibrePcbPlugin.LIBRARIES_SECTION), filings,
                command -> Thread.ofVirtual().name("libre-pcb-libraries").start(command));
        return new LibrePcbPlugin(new LibrePcbDesignReading(filings), installed, properties.libraryFolders());
    }
}
