package org.jmouse.cad.plugin.explorer;

import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.document.source.DocumentLocation.Access;
import org.jmouse.cad.jpa.mount.MountedFolderRecords;
import org.jmouse.cad.management.CadAccessRules;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.DocumentReading;
import org.jmouse.plugin.Extensions;
import org.jmouse.plugin.PluginSettingValues;
import org.jmouse.plugin.PluginSettings;
import org.jmouse.plugin.spring.PluginAutoConfiguration;
import org.jmouse.storage.local.LocalFileStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

/**
 * 🧩 The explorer plugin, on the classpath: its plugin with the server's folders, the service over every mount, and its
 * routes. Who may do what is the engine's seam ({@code CadAuthority}, {@code CadPlaces}) the host already implements
 * for the engine: nothing more is asked of it.
 *
 * <pre>{@code
 * jmouse.cad.explorer.mounts:
 *   - { id: datasheets, title: Datasheets, path: "D:/library/datasheets", access: READ_ONLY }
 *   - { id: amplifier, title: Amplifier, path: "D:/projects/amplifier", access: READ_WRITE, owner: { type: PROJECT, id: "42" } }
 * }</pre>
 */
@AutoConfiguration(after = PluginAutoConfiguration.class)
@EnableConfigurationProperties(ExplorerPluginConfiguration.ExplorerProperties.class)
public class ExplorerPluginConfiguration {

    /**
     * {@code jmouse.cad.explorer.*}.
     *
     * @param mounts the server's folders: each one's id in an address, its title, where it is on the server's disk,
     *               what it lets be done ({@code READ_ONLY}, the default, or {@code READ_WRITE}), and whose it is — none
     *               for the installation's. More are mounted from the interface by a curator, kept by the installation.
     */
    @ConfigurationProperties("jmouse.cad.explorer")
    public record ExplorerProperties(List<Mount> mounts) {

        public ExplorerProperties {
            mounts = mounts == null ? List.of() : List.copyOf(mounts);
        }

        public record Mount(String id, String title, Path path, Access access, DocumentOwner owner) {
        }
    }

    /** Each mount a local store rooted at its folder; a folder that is not there is refused at start, not at first use. */
    @Bean
    public ExplorerPlugin cadExplorerPlugin(ExplorerProperties properties, ObjectProvider<MountDocuments> documents) {
        List<ExplorerMount> mounts = properties.mounts().stream().map(mount -> {
            if (mount.path() == null || !Files.isDirectory(mount.path())) {
                throw new IllegalStateException("jmouse.cad.explorer.mounts '" + mount.id() + "' names no folder: " + mount.path());
            }
            return ExplorerMount.ofFolder(mount.id(), mount.title(), mount.owner(),
                                          new LocalFileStore(ExplorerPlugin.ID + "-" + mount.id(), mount.path()),
                                          mount.access() == null ? Access.READ_ONLY : mount.access(), mount.path());
        }).toList();
        return new ExplorerPlugin(mounts, new MountDocuments.Source(documents::getObject));
    }

    /** The folders mounted: the configured ones, and those a curator mounted, kept in the engine's tables. */
    @Bean
    public ExplorerMounts cadExplorerMounts(Extensions extensions, MountedFolderRecords records) {
        return new ExplorerMounts(extensions, records, Clock.systemUTC());
    }

    @Bean
    public MountDocuments cadMountDocuments(ExplorerMounts mounts, Extensions extensions, DocumentReading reading, PluginSettings settings) {
        PluginSettingValues values = settings.valuesOf(ExplorerPlugin.ID);
        return new MountDocuments(mounts, extensions, reading, () -> Math.toIntExact(values.wholeNumber(ExplorerPlugin.MOST_FILES)),
                () -> values.wholeNumber(ExplorerPlugin.LARGEST_DOCUMENT) * ExplorerPlugin.MEGABYTE);
    }

    @Bean
    public Explorer cadExplorer(Extensions extensions, ExplorerMounts mounts, CadAuthority authority, CadPlaces places,
                                MountDocuments documents, DocumentReading reading, CadCatalogue catalogue) {
        return new Explorer(extensions, mounts, authority, places, documents, reading, catalogue);
    }

    @Bean
    public ExplorerController cadExplorerController(Explorer explorer) {
        CadAccessRules.requireDecidedInside(ExplorerController.class);
        return new ExplorerController(explorer);
    }
}
