package org.jmouse.cad.store.settings;

import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.plugin.CadEnginePlugin;
import org.jmouse.cad.store.plugin.EngineConfiguration;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.settings.Section;
import org.jmouse.settings.SettingHolder;
import org.jmouse.settings.SettingRefused;
import org.jmouse.settings.Settings;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 🧭 What the interface keeps of how its reader works, for every product that embeds the engine: its own sections' values —
 * {@code interface/editor/grid}, {@code interface/editor/themes} —, each reader's own, under the open section {@value #BRANCH} (ADR
 * 0007: one row a value). The engine holds each row to its form and knows nothing of what it means: what a section is, its defaults
 * and its schema, are the interface's. The reader is the person signed in — {@link CadAuthority#actor()} —, never the place they work in.
 */
public class ReaderSettings {

    /** The open section every section of the interface stands under. */
    public static final String BRANCH = "interface";

    /** The most characters a row of the interface holds: one line, never a document. */
    public static final int LONGEST_VALUE = 512;

    /** The open section, declared by the engine for every interface that embeds it. */
    public static final Section SECTION = Section.open(BRANCH, "Interface",
            "What the interface keeps of how its reader works: its own sections' values, each reader's own.", Section.Open.WORDS, LONGEST_VALUE);

    /** How a reader is named as a holder of settings: the actor the host says is signed in. */
    public static final String READER = "READER";

    private final Settings            settings;
    private final EngineConfiguration configuration;
    private final CadAuthority        authority;

    public ReaderSettings(Settings settings, EngineConfiguration configuration, CadAuthority authority) {
        this.settings = settings;
        this.configuration = configuration;
        this.authority = authority;
    }

    /** Whether a path is the interface's — its open section or one below it. */
    public static boolean covers(String path) {
        return SECTION.covers(path);
    }

    /** The signed-in reader's rows of every section at or below a path of the interface's, by section path. */
    public Map<String, Map<String, String>> rowsUnder(String path) {
        return settings.ownUnder(CadEnginePlugin.ID, path, reader());
    }

    /**
     * Changes the signed-in reader's rows of one section of the interface's, all of them or none: a null or blank value forgets the
     * row. A reader keeps at most as many rows as the engine allows, in every section together.
     *
     * @return the reader's rows of that section as they now stand
     */
    public Map<String, String> change(String path, Map<String, String> changes) {
        SettingHolder reader = reader();
        Map<String, String> rows = new LinkedHashMap<>();
        changes.forEach((key, value) -> rows.put(key, value == null || value.isBlank() ? null : value.strip()));
        int kept = settings.ownUnder(CadEnginePlugin.ID, BRANCH, reader).entrySet().stream()
                .filter(section -> !section.getKey().equals(path)).mapToInt(section -> section.getValue().size()).sum();
        Map<String, String> own = new HashMap<>(settings.ownOf(CadEnginePlugin.ID, path, reader));
        rows.forEach((key, value) -> {
            if (value == null) {
                own.remove(key);
            } else {
                own.put(key, value);
            }
        });
        int most = configuration.mostValues();
        if (kept + own.size() > most) {
            throw CadProblem.invalid("A reader keeps at most " + most + " values of the interface's; an interface naming more names something it should not.");
        }
        try {
            settings.change(CadEnginePlugin.ID, path, reader, rows);
        } catch (SettingRefused refused) {
            throw CadProblem.invalid(refused.getMessage());
        }
        return settings.ownOf(CadEnginePlugin.ID, path, reader);
    }

    /** Forgets every row the signed-in reader keeps of one section of the interface's: it reads as the interface ships it again. */
    public void forget(String path) {
        Map<String, String> forgotten = new HashMap<>();
        settings.ownOf(CadEnginePlugin.ID, path, reader()).keySet().forEach(key -> forgotten.put(key, null));
        if (!forgotten.isEmpty()) {
            change(path, forgotten);
        }
    }

    /** The signed-in reader, as a holder of settings. */
    public SettingHolder reader() {
        return SettingHolder.reader(READER, authority.requireActor());
    }
}
