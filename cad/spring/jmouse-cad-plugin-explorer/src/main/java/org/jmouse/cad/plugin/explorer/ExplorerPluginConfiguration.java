package org.jmouse.cad.plugin.explorer;

import org.jmouse.cad.jpa.catalogue.DocumentOwner;
import org.jmouse.cad.management.CadAccessRules;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.plugin.Extensions;
import org.jmouse.plugin.spring.PluginAutoConfiguration;
import org.jmouse.storage.local.LocalFileStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 🧩 The explorer plugin, on the classpath: its plugin with the server's folders, the service over every mount, and its
 * routes. Who may do what is the engine's seam ({@code CadAuthority}, {@code CadPlaces}) the host already implements
 * for the engine: nothing more is asked of it.
 *
 * <pre>{@code
 * jmouse.cad.explorer.mounts:
 *   - { id: datasheets, title: Datasheets, path: "D:/library/datasheets", writable: false }
 *   - { id: amplifier, title: Amplifier, path: "D:/projects/amplifier", writable: true, owner: { type: PROJECT, id: "42" } }
 * }</pre>
 */
@AutoConfiguration(after = PluginAutoConfiguration.class)
@EnableConfigurationProperties(ExplorerPluginConfiguration.ExplorerProperties.class)
public class ExplorerPluginConfiguration {

    /**
     * {@code jmouse.cad.explorer.*}.
     *
     * @param mounts the server's folders: each one's id in an address, its title, where it is on the server's disk,
     *               whether it may be changed at all, and whose it is — none for the installation's
     */
    @ConfigurationProperties("jmouse.cad.explorer")
    public record ExplorerProperties(List<Mount> mounts) {

        public ExplorerProperties {
            mounts = mounts == null ? List.of() : List.copyOf(mounts);
        }

        public record Mount(String id, String title, Path path, boolean writable, DocumentOwner owner) {
        }
    }

    /** Each mount a local store rooted at its folder; a folder that is not there is refused at start, not at first use. */
    @Bean
    public ExplorerPlugin cadExplorerPlugin(ExplorerProperties properties) {
        List<ExplorerMount> mounts = properties.mounts().stream().map(mount -> {
            if (mount.path() == null || !Files.isDirectory(mount.path())) {
                throw new IllegalStateException("jmouse.cad.explorer.mounts '" + mount.id() + "' names no folder: " + mount.path());
            }
            return ExplorerMount.of(mount.id(), mount.title(), mount.owner(),
                                    new LocalFileStore(ExplorerPlugin.ID + "-" + mount.id(), mount.path()), mount.writable());
        }).toList();
        return new ExplorerPlugin(mounts);
    }

    @Bean
    public Explorer cadExplorer(Extensions extensions, CadAuthority authority, CadPlaces places) {
        return new Explorer(extensions, authority, places);
    }

    @Bean
    public ExplorerController cadExplorerController(Explorer explorer) {
        CadAccessRules.requireDecidedInside(ExplorerController.class);
        return new ExplorerController(explorer);
    }
}
