package org.jmouse.cad.store.plugin;

import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadPlace;
import org.jmouse.cad.store.authority.Capability;
import org.jmouse.cad.store.plugin.EnginePluginSettings.SettingView;
import org.jmouse.plugin.Contributions;
import org.jmouse.plugin.Decorations;
import org.jmouse.plugin.ExtensionPoint;
import org.jmouse.plugin.Extensions;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;
import org.jmouse.plugin.PluginSetting;
import org.jmouse.plugin.PluginSettingStore;
import org.jmouse.plugin.PluginSettings;
import org.jmouse.plugin.PluginState;
import org.jmouse.plugin.Topic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What names the server's folders and programs is a way in: it is told, and listed, only to who may set the engine up. */
class EnginePluginSettingsTest {

    private static final String PLUGIN = "holding";

    /** Refused by a test's authority: what it stands for is that the caller was stopped, however a product says so. */
    private static final class Stopped extends RuntimeException {
    }

    private static CadAuthority authority(boolean curator) {
        return new CadAuthority() {
            @Override
            public Optional<String> actor() {
                return Optional.of("reader");
            }

            @Override
            public String requireActor() {
                return "reader";
            }

            @Override
            public boolean permits(Capability capability, CadPlace place) {
                return curator;
            }

            @Override
            public void require(Capability capability, CadPlace place) {
                if (!curator) {
                    throw new Stopped();
                }
            }
        };
    }

    private static EnginePluginSettings settings(boolean curator, Path folder) {
        Plugin plugin = new Plugin() {
            @Override
            public PluginDescriptor descriptor() {
                return PluginDescriptor.of(PLUGIN, "Holding", "1");
            }

            @Override
            public void register(PluginRegistration registration) {
            }

            @Override
            public List<PluginSetting> settings() {
                return List.of(PluginSetting.folders("libraries", "Libraries", null, List.of(folder, folder.resolve("absent"))),
                               PluginSetting.command("program", "Program", null, List.of("/usr/bin/program", "-b")),
                               PluginSetting.text("label", "Label", null, "plain"));
            }
        };
        PluginState started = new PluginState(plugin.descriptor(), PluginState.Status.STARTED, null, Set.of());
        Extensions extensions = new Extensions() {
            @Override
            public <T> List<T> contributions(Contributions<T> point) {
                return List.of();
            }

            @Override
            public <T> T decorated(Decorations<T> point, T service) {
                return service;
            }

            @Override
            public <T> void publish(Topic<T> topic, T event) {
            }

            @Override
            public List<PluginState> plugins() {
                return List.of(started);
            }

            @Override
            public List<ExtensionPoint<?>> points() {
                return List.of();
            }
        };
        Map<String, Map<String, String>> kept = new HashMap<>();
        PluginSettingStore store = new PluginSettingStore() {
            @Override
            public Map<String, String> changed(String owner) {
                return kept.getOrDefault(owner, Map.of());
            }

            @Override
            public void keep(String owner, Map<String, String> values) {
                kept.computeIfAbsent(owner, unknown -> new HashMap<>()).putAll(values);
            }
        };
        return new EnginePluginSettings(extensions, new PluginSettings(() -> List.of(plugin), Optional.of(store)), authority(curator));
    }

    private static SettingView settingOf(EnginePluginSettings settings, String key) {
        return settings.settings().plugins().getFirst().settings().stream()
                .filter(setting -> setting.key().equals(key)).findFirst().orElseThrow();
    }

    @Test
    void aReaderWhoMayNotSetTheEngineUpIsToldNothingOfTheServer(@TempDir Path folder) {
        EnginePluginSettings settings = settings(false, folder);
        for (String key : List.of("libraries", "program")) {
            SettingView setting = settingOf(settings, key);
            assertEquals("", setting.value(), key);
            assertEquals("", setting.defaultValue(), key);
            assertEquals(List.of(), setting.absent(), key);
        }
        assertEquals("plain", settingOf(settings, "label").value());
        assertFalse(settings.settings().changeable());
    }

    @Test
    void aReaderWhoMayNotSetTheEngineUpIsListedNoFolderAndChangesNothing(@TempDir Path folder) {
        EnginePluginSettings settings = settings(false, folder);
        assertThrows(Stopped.class, settings::serverRoots);
        assertThrows(Stopped.class, () -> settings.serverFolder(folder.toString()));
        assertThrows(Stopped.class, () -> settings.change(PLUGIN, Map.of("program", "/usr/bin/other")));
    }

    @Test
    void whoMaySetTheEngineUpSeesThemAndWhichFolderIsNotThere(@TempDir Path folder) {
        EnginePluginSettings settings = settings(true, folder);
        SettingView libraries = settingOf(settings, "libraries");
        assertTrue(libraries.value().contains(folder.toString()));
        assertEquals(List.of(folder.resolve("absent").toString()), libraries.absent());
        assertEquals("/usr/bin/program\n-b", settingOf(settings, "program").value());
        assertEquals(folder.toString(), settings.serverFolder(folder.toString()).path());
    }
}
