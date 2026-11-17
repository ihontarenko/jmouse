package org.jmouse.cad.store.plugin;

import org.jmouse.cad.document.DocumentOwner;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadPlace;
import org.jmouse.cad.store.authority.Capability;
import org.jmouse.cad.store.plugin.EnginePluginSettings.ParameterView;
import org.jmouse.plugin.Contributions;
import org.jmouse.plugin.Decorations;
import org.jmouse.plugin.ExtensionPoint;
import org.jmouse.plugin.Extensions;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;
import org.jmouse.plugin.PluginState;
import org.jmouse.plugin.Topic;
import org.jmouse.settings.Parameter;
import org.jmouse.settings.ParameterStore;
import org.jmouse.settings.Section;
import org.jmouse.settings.SettingHolder;
import org.jmouse.settings.Settings;
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
    private static final String SECTION = "programs";

    /** Refused by a test's authority: what it stands for is that the caller was stopped, however a product says so. */
    private static final class Stopped extends RuntimeException {
    }

    private static CadAuthority authority(boolean curator) {
        return authority(curator, true);
    }

    private static CadAuthority authority(boolean curator, boolean signedIn) {
        return new CadAuthority() {
            @Override
            public Optional<String> actor() {
                return signedIn ? Optional.of("reader") : Optional.empty();
            }

            @Override
            public String requireActor() {
                return "reader";
            }

            @Override
            public Optional<DocumentOwner> actorOwner() {
                return Optional.empty();
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
        return settings(authority(curator), folder);
    }

    private static EnginePluginSettings settings(CadAuthority authority, Path folder) {
        Plugin plugin = new Plugin() {
            @Override
            public PluginDescriptor descriptor() {
                return PluginDescriptor.of(PLUGIN, "Holding", "1");
            }

            @Override
            public void register(PluginRegistration registration) {
            }

            @Override
            public List<Section> settings() {
                return List.of(Section.fixed(SECTION, "Holding", null, List.of(
                        Parameter.folders("libraries", "Libraries", null, List.of(folder, folder.resolve("absent"))),
                        Parameter.command("program", "Program", null, List.of("/usr/bin/program", "-b")),
                        Parameter.text("label", "Label", null, "plain"))));
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
        Map<String, String> kept = new HashMap<>();
        ParameterStore store = new ParameterStore() {
            @Override
            public Map<String, String> kept(SettingHolder holder, String owner, String section) {
                return Map.copyOf(kept);
            }

            @Override
            public void keep(SettingHolder holder, String owner, String section, Map<String, String> values) {
                kept.putAll(values);
            }

            @Override
            public void forget(SettingHolder holder, String owner, String section, java.util.Set<String> keys) {
                keys.forEach(kept::remove);
            }

            @Override
            public void replace(SettingHolder holder, String owner, String section, Map<String, String> values) {
                kept.clear();
                kept.putAll(values);
            }

            @Override
            public Map<String, Map<String, String>> keptUnder(SettingHolder holder, String owner, String section) {
                return kept.isEmpty() ? Map.of() : Map.of(section, Map.copyOf(kept));
            }
        };
        return new EnginePluginSettings(extensions, new Settings(() -> Map.of(PLUGIN, plugin.settings()), Optional.of(store),
                                                                 Optional.empty(), Optional.empty()), authority);
    }

    private static ParameterView settingOf(EnginePluginSettings settings, String key) {
        return settings.parametersOf(settings.fixedSections().getFirst()).stream()
                .filter(setting -> setting.key().equals(key)).findFirst().orElseThrow();
    }

    @Test
    void aReaderWhoMayNotSetTheEngineUpIsToldNothingOfTheServer(@TempDir Path folder) {
        EnginePluginSettings settings = settings(false, folder);
        for (String key : List.of("libraries", "program")) {
            ParameterView setting = settingOf(settings, key);
            assertEquals("", setting.value(), key);
            assertEquals("", setting.defaultValue(), key);
            assertEquals(List.of(), setting.absent(), key);
        }
        assertEquals("plain", settingOf(settings, "label").value());
        assertFalse(settings.changeable());
    }

    @Test
    void aReaderWhoMayNotSetTheEngineUpIsListedNoFolderAndChangesNothing(@TempDir Path folder) {
        EnginePluginSettings settings = settings(false, folder);
        assertThrows(Stopped.class, settings::serverRoots);
        assertThrows(Stopped.class, () -> settings.serverFolder(folder.toString()));
        assertThrows(Stopped.class, () -> settings.changeSection(PLUGIN, SECTION, Map.of("program", "/usr/bin/other")));
    }

    @Test
    void aVisitorWhoIsNotSignedInIsShownNoSectionOfParameters(@TempDir Path folder) {
        assertEquals(List.of(), settings(authority(false, false), folder).fixedSections());
    }

    @Test
    void aSectionOfParametersStandsUnderItsPluginInTheTree(@TempDir Path folder) {
        EnginePluginSettings settings = settings(true, folder);
        assertEquals("plugin/" + PLUGIN + "/" + SECTION, settings.fixedSections().getFirst().path());
        assertFalse(settings.changed(settings.fixedSections().getFirst()));
    }

    @Test
    void whoMaySetTheEngineUpSeesThemAndWhichFolderIsNotThere(@TempDir Path folder) {
        EnginePluginSettings settings = settings(true, folder);
        ParameterView libraries = settingOf(settings, "libraries");
        assertTrue(libraries.value().contains(folder.toString()));
        assertEquals(List.of(folder.resolve("absent").toString()), libraries.absent());
        assertEquals("/usr/bin/program\n-b", settingOf(settings, "program").value());
        assertEquals(folder.toString(), settings.serverFolder(folder.toString()).path());
    }
}
