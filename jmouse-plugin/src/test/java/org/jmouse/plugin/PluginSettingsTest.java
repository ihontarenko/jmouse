package org.jmouse.plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginSettingsTest {

    /** A plugin holding a key, and a sealing that a test can tell from the clear. */
    private static final Plugin KEYED = new Plugin() {
        @Override
        public PluginDescriptor descriptor() {
            return PluginDescriptor.of("keyed", "Keyed", "1");
        }

        @Override
        public void register(PluginRegistration registration) {
        }

        @Override
        public List<PluginSetting> settings() {
            return List.of(PluginSetting.secret("token", "Token", null, "deployed-token").inGroup("Connection"));
        }
    };

    private static final PluginSecrets SEALING = new PluginSecrets() {
        @Override
        public String seal(String secret, String context) {
            return "sealed:" + context + ":" + secret;
        }

        @Override
        public String open(String sealed, String context) {
            assertTrue(sealed.startsWith("sealed:" + context + ":"), "opened under another setting");
            return sealed.substring(("sealed:" + context + ":").length());
        }
    };

    private static PluginSettings keyed(PluginSettingStore store, boolean sealing) {
        return new PluginSettings(() -> List.of(KEYED), Optional.of(store), sealing ? Optional.of(SEALING) : Optional.empty());
    }

    @Test
    void aSecretIsSealedWhereItIsKeptAndNeverReadBack() {
        Kept store = new Kept();
        PluginSettings settings = keyed(store, true);
        PluginSetting token = KEYED.settings().getFirst();
        assertEquals(PluginSettings.SecretState.DEPLOYED, settings.secretStateOf("keyed", token));
        assertEquals("deployed-token", settings.valuesOf("keyed").secret("token"));
        assertFalse(settings.currentOf("keyed").containsKey("token"));

        settings.change("keyed", Map.of("token", "  my-token "));
        assertTrue(store.values.get("keyed").get("token").startsWith("sealed:"));
        assertEquals(PluginSettings.SecretState.KEPT, settings.secretStateOf("keyed", token));
        assertEquals("my-token", settings.valuesOf("keyed").secret("token"));
        assertFalse(settings.currentOf("keyed").containsKey("token"));
    }

    @Test
    void aSecretIsForgottenAndTheDeploymentsStandsAgain() {
        Kept store = new Kept();
        PluginSettings settings = keyed(store, true);
        settings.change("keyed", Map.of("token", "my-token"));
        Map<String, String> forget = new HashMap<>();
        forget.put("token", null);
        settings.change("keyed", forget);
        assertEquals("deployed-token", settings.valuesOf("keyed").secret("token"));
    }

    @Test
    void withNothingToSealWithASecretIsRefusedNotKeptInTheClear() {
        Kept store = new Kept();
        PluginSettings settings = keyed(store, false);
        assertThrows(PluginSettingRefused.class, () -> settings.change("keyed", Map.of("token", "my-token")));
        assertFalse(store.values.containsKey("keyed"));
        assertFalse(settings.keepsSecrets());
    }

    /** A plugin of two settings: how many parts a search asks for, and how long a run may take. */
    private static final Plugin SEARCH = new Plugin() {
        @Override
        public PluginDescriptor descriptor() {
            return PluginDescriptor.of("parts-search", "Parts search", "1");
        }

        @Override
        public void register(PluginRegistration registration) {
        }

        @Override
        public List<PluginSetting> settings() {
            return List.of(PluginSetting.wholeNumber("limit", "Parts per search", null, 10, 1, 100, "parts"),
                           PluginSetting.duration("timeout", "Longest run", null, Duration.ofSeconds(60),
                                                  Duration.ofSeconds(1), Duration.ofMinutes(10)));
        }
    };

    /** A store in memory, as a test keeps it. */
    private static final class Kept implements PluginSettingStore {
        final Map<String, Map<String, String>> values = new HashMap<>();
        int reads;

        @Override
        public Map<String, String> changed(String plugin) {
            reads++;
            return values.getOrDefault(plugin, Map.of());
        }

        @Override
        public void keep(String plugin, Map<String, String> changes) {
            Map<String, String> kept = values.computeIfAbsent(plugin, unknown -> new HashMap<>());
            changes.forEach((key, value) -> {
                if (value == null) {
                    kept.remove(key);
                } else {
                    kept.put(key, value);
                }
            });
        }
    }

    @Test
    void aSettingStandsAtItsDefaultUntilChangedAndBackOnceForgotten() {
        Kept store = new Kept();
        PluginSettings settings = new PluginSettings(() -> List.of(SEARCH), Optional.of(store));
        PluginSettingValues values = settings.valuesOf("parts-search");
        assertEquals(10, values.wholeNumber("limit"));

        settings.change("parts-search", Map.of("limit", "24"));
        assertEquals(24, values.wholeNumber("limit"));
        assertEquals(Map.of("limit", "24", "timeout", "PT1M"), settings.currentOf("parts-search"));

        Map<String, String> forgotten = new HashMap<>();
        forgotten.put("limit", null);
        settings.change("parts-search", forgotten);
        assertEquals(10, values.wholeNumber("limit"));
    }

    @Test
    void aChangeIsReadOnceNotOnEveryUse() {
        Kept store = new Kept();
        PluginSettings settings = new PluginSettings(() -> List.of(SEARCH), Optional.of(store));
        PluginSettingValues values = settings.valuesOf("parts-search");
        values.wholeNumber("limit");
        values.duration("timeout");
        assertEquals(1, store.reads);
    }

    @Test
    void aValueOutsideItsSettingIsRefusedAndNothingIsKept() {
        Kept store = new Kept();
        PluginSettings settings = new PluginSettings(() -> List.of(SEARCH), Optional.of(store));
        PluginSettingRefused tooMany = assertThrows(PluginSettingRefused.class,
                () -> settings.change("parts-search", Map.of("limit", "500")));
        assertTrue(tooMany.getMessage().contains("at most 100"), tooMany.getMessage());
        assertThrows(PluginSettingRefused.class, () -> settings.change("parts-search", Map.of("limit", "2.5")));
        assertThrows(PluginSettingRefused.class, () -> settings.change("parts-search", Map.of("timeout", "PT1H")));
        assertThrows(PluginSettingRefused.class, () -> settings.change("parts-search", Map.of("colour", "red")));
        assertTrue(store.values.isEmpty());
    }

    @Test
    void withoutAStoreEverySettingIsItsDefaultAndAChangeIsRefused() {
        PluginSettings settings = new PluginSettings(() -> List.of(SEARCH), Optional.empty());
        assertFalse(settings.changeable());
        assertEquals(Duration.ofMinutes(1), settings.valuesOf("parts-search").duration("timeout"));
        assertThrows(PluginSettingRefused.class, () -> settings.change("parts-search", Map.of("limit", "20")));
    }

    @Test
    void aDeclarationRefusesADefaultItWouldRefuse() {
        assertThrows(IllegalArgumentException.class,
                     () -> PluginSetting.wholeNumber("limit", "Parts", null, 500, 1, 100, null));
        assertThrows(IllegalArgumentException.class,
                     () -> PluginSetting.choice("mode", "Mode", null, "fast", List.of(new PluginSetting.Choice("slow", "Slow"))));
    }

    @Test
    void aSettingOfFoldersReadsItsLinesAndRefusesAPartOfAPath(@TempDir Path folder) {
        Plugin plugin = holding(PluginSetting.folders("libraries", "Libraries", null, List.of()));
        Kept store = new Kept();
        PluginSettings settings = new PluginSettings(() -> List.of(plugin), Optional.of(store));
        assertEquals(List.of(), settings.valuesOf("holding").folders("libraries"));
        settings.change("holding", Map.of("libraries", folder + "\n\n  " + folder.resolve("more") + "  "));
        assertEquals(List.of(folder, folder.resolve("more")), settings.valuesOf("holding").folders("libraries"));
        PluginSettingRefused refused = assertThrows(PluginSettingRefused.class,
                () -> settings.change("holding", Map.of("libraries", "relative/folder")));
        assertTrue(refused.getMessage().contains("relative/folder"), refused.getMessage());
    }

    @Test
    void aCommandIsWordsAndAWordMayHoldSpaces() {
        Plugin plugin = holding(PluginSetting.command("program", "Program", null, List.of("docker", "run")));
        Kept store = new Kept();
        PluginSettings settings = new PluginSettings(() -> List.of(plugin), Optional.of(store));
        assertEquals(List.of("docker", "run"), settings.valuesOf("holding").command("program"));
        settings.change("holding", Map.of("program", "C:/Program Files/Spice/ngspice.exe\n-b"));
        assertEquals(List.of("C:/Program Files/Spice/ngspice.exe", "-b"), settings.valuesOf("holding").command("program"));
        settings.change("holding", Map.of("program", ""));
        assertEquals(List.of(), settings.valuesOf("holding").command("program"));
    }

    @Test
    void whatNamesTheServerIsToldApartFromTheRest() {
        assertTrue(PluginSetting.Kind.FOLDERS.namesTheServer());
        assertTrue(PluginSetting.Kind.COMMAND.namesTheServer());
        assertFalse(PluginSetting.Kind.TEXT.namesTheServer());
        assertFalse(PluginSetting.Kind.SECRET.namesTheServer());
    }

    @Test
    void aFolderListsOnlyTheFoldersInItAndWhereItIs(@TempDir Path folder) throws IOException {
        Files.createDirectory(folder.resolve("beta"));
        Files.createDirectory(folder.resolve("Alpha"));
        Files.writeString(folder.resolve("secret.txt"), "never named");
        ServerFolders.Listing listing = ServerFolders.listing(folder.toString());
        assertEquals(List.of("Alpha", "beta"), listing.folders().stream().map(ServerFolders.Folder::name).toList());
        assertEquals(folder.getParent().toString(), listing.parent());
        assertFalse(listing.truncated());
        assertThrows(IllegalArgumentException.class, () -> ServerFolders.listing("relative/folder"));
        assertThrows(IllegalArgumentException.class, () -> ServerFolders.listing(folder.resolve("secret.txt").toString()));
        assertFalse(ServerFolders.roots().isEmpty());
    }

    private static Plugin holding(PluginSetting setting) {
        return new Plugin() {
            @Override
            public PluginDescriptor descriptor() {
                return PluginDescriptor.of("holding", "Holding", "1");
            }

            @Override
            public void register(PluginRegistration registration) {
            }

            @Override
            public List<PluginSetting> settings() {
                return List.of(setting);
            }
        };
    }
}
