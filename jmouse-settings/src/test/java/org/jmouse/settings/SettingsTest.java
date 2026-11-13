package org.jmouse.settings;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsTest {

    /** A store in memory: each holder's section a map of rows. */
    private static final class Rows implements ParameterStore {
        final Map<String, Map<String, String>> sections = new HashMap<>();
        int reads;

        private Map<String, String> of(SettingHolder holder, String owner, String section) {
            return sections.computeIfAbsent(holder.type() + ":" + holder.id() + "/" + owner + "/" + section, unknown -> new LinkedHashMap<>());
        }

        Map<String, String> installation(String owner, String section) {
            return of(SettingHolder.INSTALLATION, owner, section);
        }

        @Override
        public Map<String, String> kept(SettingHolder holder, String owner, String section) {
            reads++;
            return new LinkedHashMap<>(of(holder, owner, section));
        }

        @Override
        public void keep(SettingHolder holder, String owner, String section, Map<String, String> values) {
            of(holder, owner, section).putAll(values);
        }

        @Override
        public void forget(SettingHolder holder, String owner, String section, Set<String> keys) {
            keys.forEach(of(holder, owner, section)::remove);
        }

        @Override
        public void replace(SettingHolder holder, String owner, String section, Map<String, String> values) {
            Map<String, String> rows = of(holder, owner, section);
            rows.clear();
            rows.putAll(values);
        }
    }

    private static final Section SEARCH = Section.fixed("search", "Part search", null, List.of(
            Parameter.wholeNumber("limit", "Parts per search", null, 10, 1, 100, "parts"),
            Parameter.duration("timeout", "Longest run", null, Duration.ofSeconds(60), Duration.ofSeconds(1), Duration.ofMinutes(10)),
            Parameter.secret("token", "Token", null, "deployed-token")));

    private static final SecretSealing SEALING = new SecretSealing() {
        @Override
        public String seal(String secret, String context) {
            return "sealed:" + context + ":" + secret;
        }

        @Override
        public String open(String sealed, String context) {
            assertTrue(sealed.startsWith("sealed:" + context + ":"), "opened under another parameter");
            return sealed.substring(("sealed:" + context + ":").length());
        }
    };

    /** A record a structured section is read into, and a reader that reads it from its folded tree. */
    record Layout(double gap, List<Integer> stacks) {
    }

    private static final SectionReader READER = new SectionReader() {
        @Override
        @SuppressWarnings("unchecked")
        public <T> T read(Object tree, Class<T> type) {
            Map<String, Object> record = (Map<String, Object>) tree;
            if (!(record.get("gap") instanceof String gap) || !(record.get("stacks") instanceof List<?> stacks)) {
                throw new IllegalArgumentException("a layout has a gap and stacks");
            }
            try {
                return type.cast(new Layout(Double.parseDouble(gap), stacks.stream().map(stack -> Integer.parseInt((String) stack)).toList()));
            } catch (NumberFormatException notANumber) {
                throw new IllegalArgumentException("gap: '" + gap + "' is not a number");
            }
        }
    };

    private static final Section LAYOUT = Section.structured("pcb/layout", "PCB layout", null, Layout.class,
            ParameterTree.unfold(Map.of("gap", 1.5, "stacks", List.of(1, 2, 4))));

    private static Settings settingsOf(Rows rows, Section... sections) {
        return new Settings(() -> Map.of("engine", List.of(sections)), Optional.of(rows), Optional.of(SEALING), Optional.of(READER));
    }

    @Test
    void theFirstStartKeepsEveryValueOneRowEachAndNoSecret() {
        Rows rows = new Rows();
        Settings settings = settingsOf(rows, SEARCH, LAYOUT);
        assertEquals(6, settings.seed());
        assertEquals(Map.of("limit", "10", "timeout", "PT1M"), rows.installation("engine", "search"));
        assertEquals(Map.of("gap", "1.5", "stacks.0", "1", "stacks.1", "2", "stacks.2", "4"), rows.installation("engine", "pcb/layout"));
        assertEquals(0, settingsOf(rows, SEARCH, LAYOUT).seed(), "a value kept is never seeded again");
    }

    @Test
    void aStructuredSectionIsReadFromItsRowsAndAChangeAppliesAtOnce() {
        Rows rows = new Rows();
        Settings settings = settingsOf(rows, LAYOUT);
        settings.seed();
        assertEquals(new Layout(1.5, List.of(1, 2, 4)), settings.read("engine", "pcb/layout", Layout.class));
        settings.change("engine", "pcb/layout", SettingHolder.INSTALLATION, Map.of("gap", "2"));
        assertEquals("2", rows.installation("engine", "pcb/layout").get("gap"));
        assertEquals(new Layout(2, List.of(1, 2, 4)), settings.read("engine", "pcb/layout", Layout.class));
    }

    @Test
    void aChangeTheRecordCannotReadIsRefusedAndNothingIsKept() {
        Rows rows = new Rows();
        Settings settings = settingsOf(rows, LAYOUT);
        settings.seed();
        SettingRefused refused = assertThrows(SettingRefused.class,
                () -> settings.change("engine", "pcb/layout", SettingHolder.INSTALLATION, Map.of("gap", "wide")));
        assertTrue(refused.getMessage().contains("gap: 'wide' is not a number"), refused.getMessage());
        assertEquals("1.5", rows.installation("engine", "pcb/layout").get("gap"));
    }

    @Test
    void anItemTakenAwayStaysAwayAndANewKeyOfAReleaseIsSeeded() {
        Rows rows = new Rows();
        Settings settings = settingsOf(rows, LAYOUT);
        settings.seed();
        Map<String, String> shorter = new LinkedHashMap<>(rows.installation("engine", "pcb/layout"));
        shorter.remove("stacks.2");
        settings.replace("engine", "pcb/layout", SettingHolder.INSTALLATION, shorter);
        Section released = Section.structured("pcb/layout", "PCB layout", null, Layout.class,
                ParameterTree.unfold(Map.of("gap", 1.5, "stacks", List.of(1, 2, 4), "margin", 3)));
        assertEquals(1, new Settings(() -> Map.of("engine", List.of(released)), Optional.of(rows), Optional.empty(), Optional.of(READER)).seed());
        assertFalse(rows.installation("engine", "pcb/layout").containsKey("stacks.2"));
        assertEquals("3", rows.installation("engine", "pcb/layout").get("margin"));
    }

    @Test
    void aValueBroughtBackKeepsItsSeedAndARestoreTheWholeSection() {
        Rows rows = new Rows();
        Settings settings = settingsOf(rows, SEARCH, LAYOUT);
        settings.seed();
        settings.change("engine", "search", SettingHolder.INSTALLATION, Map.of("limit", "24"));
        Map<String, String> back = new HashMap<>();
        back.put("limit", null);
        settings.change("engine", "search", SettingHolder.INSTALLATION, back);
        assertEquals("10", rows.installation("engine", "search").get("limit"));
        settings.change("engine", "pcb/layout", SettingHolder.INSTALLATION, Map.of("gap", "3"));
        settings.restore("engine", "pcb/layout", SettingHolder.INSTALLATION);
        assertEquals(new Layout(1.5, List.of(1, 2, 4)), settings.read("engine", "pcb/layout", Layout.class));
    }

    @Test
    void aValueOutsideItsParameterIsRefusedAndAnUnknownKeyToo() {
        Rows rows = new Rows();
        Settings settings = settingsOf(rows, SEARCH);
        SettingRefused tooMany = assertThrows(SettingRefused.class,
                () -> settings.change("engine", "search", SettingHolder.INSTALLATION, Map.of("limit", "500")));
        assertTrue(tooMany.getMessage().contains("at most 100"), tooMany.getMessage());
        assertThrows(SettingRefused.class, () -> settings.change("engine", "search", SettingHolder.INSTALLATION, Map.of("colour", "red")));
        assertTrue(rows.installation("engine", "search").isEmpty());
    }

    @Test
    void aSecretIsSealedNeverToldAndForgottenBackToTheDeployments() {
        Rows rows = new Rows();
        Settings settings = settingsOf(rows, SEARCH);
        Parameter token = ((Section.Fixed) SEARCH.shape()).parameters().get(2);
        assertEquals(Settings.SecretState.DEPLOYED, settings.secretStateOf("engine", "search", token));
        assertEquals("deployed-token", settings.parameters("engine", "search").secret("token"));
        settings.change("engine", "search", SettingHolder.INSTALLATION, Map.of("token", "  mine "));
        assertTrue(rows.installation("engine", "search").get("token").startsWith("sealed:"));
        assertEquals("mine", settings.parameters("engine", "search").secret("token"));
        assertFalse(settings.valuesOf("engine", "search").containsKey("token"));
        settings.change("engine", "search", SettingHolder.INSTALLATION, Map.of("token", ""));
        assertEquals("deployed-token", settings.parameters("engine", "search").secret("token"));
    }

    @Test
    void aReaderOverridesKeyByKeyAndKeepsOnlyWhatIsTheirs() {
        Section look = Section.fixed("look", "Look", null, List.of(
                Parameter.choice("theme", "Theme", null, "dark", List.of(new Parameter.Choice("dark", "Dark"), new Parameter.Choice("light", "Light"))),
                Parameter.wholeNumber("scale", "Scale", null, 100, 50, 200, "%"))).forEachReader();
        Rows rows = new Rows();
        Settings settings = settingsOf(rows, look, SEARCH);
        settings.seed();
        SettingHolder reader = SettingHolder.reader("READER", "1");
        settings.change("engine", "look", reader, Map.of("theme", "light"));
        assertEquals(Map.of("theme", "light", "scale", "100"), settings.valuesOf("engine", "look", reader));
        assertEquals(Map.of("theme", "light"), settings.ownOf("engine", "look", reader));
        assertEquals("dark", settings.parameters("engine", "look", SettingHolder.reader("READER", "2")).text("theme"));
        assertThrows(SettingRefused.class, () -> settings.change("engine", "search", reader, Map.of("limit", "5")));
        Map<String, String> back = new HashMap<>();
        back.put("theme", null);
        settings.change("engine", "look", reader, back);
        assertEquals(Map.of(), settings.ownOf("engine", "look", reader));
    }

    @Test
    void withoutAStoreEveryValueIsItsSeedAndAChangeIsRefused() {
        Settings settings = new Settings(() -> Map.of("engine", List.of(SEARCH, LAYOUT)), Optional.empty(), Optional.empty(), Optional.of(READER));
        assertFalse(settings.changeable());
        assertEquals(Duration.ofMinutes(1), settings.parameters("engine", "search").duration("timeout"));
        assertEquals(new Layout(1.5, List.of(1, 2, 4)), settings.read("engine", "pcb/layout", Layout.class));
        assertThrows(SettingRefused.class, () -> settings.change("engine", "search", SettingHolder.INSTALLATION, Map.of("limit", "20")));
    }

    @Test
    void aValueIsReadOnceAndAgainOnlyAfterAChange() {
        Rows rows = new Rows();
        Settings settings = settingsOf(rows, SEARCH);
        settings.seed();
        settings.parameters("engine", "search").wholeNumber("limit");
        int once = rows.reads;
        settings.parameters("engine", "search").duration("timeout");
        settings.valuesOf("engine", "search");
        assertEquals(once, rows.reads, "read once until it changes");
        settings.change("engine", "search", SettingHolder.INSTALLATION, Map.of("limit", "12"));
        assertEquals(12, settings.parameters("engine", "search").wholeNumber("limit"));
        assertEquals(once + 1, rows.reads, "read again once after a change");
    }
}
