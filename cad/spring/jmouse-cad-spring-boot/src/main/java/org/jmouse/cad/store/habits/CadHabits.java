package org.jmouse.cad.store.habits;

import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.plugin.EngineConfiguration;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.settings.ParameterTree;
import org.jmouse.settings.SettingHolder;
import org.jmouse.settings.SettingRefused;
import org.jmouse.settings.Settings;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 🧭 A reader's habits, kept by the engine for every product that embeds it: what the interface remembers of how its reader works.
 * They are the section {@link #SECTION}, each habit one row ({@code habits[pcb.look]} = {@code classic}, ADR 0007): the
 * installation's are what every reader starts from, and a reader keeps a row only of a habit they changed. The reader is the person
 * signed in — {@link CadAuthority#actor()} —, never the place they work in: a workspace shared by many keeps nobody's column widths.
 */
public class CadHabits {

    /** The habits: a section of the engine's tree each reader holds over the installation's. */
    public static final SettingsSection<InterfaceHabits> SECTION = new SettingsSection<>("interface/habits", "Interface habits", InterfaceHabits.class);

    /** How a reader is named as a holder of settings: the actor the host says is signed in. */
    public static final String READER = "READER";

    /** The record's one field the habits stand under: {@code habits[pcb.look]}. */
    private static final String HABITS = "habits";

    private final Settings            settings;
    private final EngineConfiguration configuration;
    private final CadAuthority        authority;

    public CadHabits(Settings settings, EngineConfiguration configuration, CadAuthority authority) {
        this.settings = settings;
        this.configuration = configuration;
        this.authority = authority;
    }

    /** The signed-in reader's habits: the installation's, each one the reader changed as the reader has it. */
    public Map<String, String> habits() {
        return settings.read(SECTION.plugin(), SECTION.pathInOwner(), InterfaceHabits.class, reader()).habits();
    }

    /**
     * Changes the signed-in reader's habits, all of them or none: a blank or null value forgets the reader's own, which then reads
     * as the installation's.
     *
     * @return the reader's habits as they now stand
     */
    public Map<String, String> change(Map<String, String> changes) {
        SettingHolder reader = reader();
        Map<String, String> rows = new LinkedHashMap<>();
        changes.forEach((habit, value) -> rows.put(keyOf(habit), value == null || value.isBlank() ? null : value.strip()));
        Map<String, String> own = new HashMap<>(settings.ownOf(SECTION.plugin(), SECTION.pathInOwner(), reader));
        rows.forEach((key, value) -> {
            if (value == null) {
                own.remove(key);
            } else {
                own.put(key, value);
            }
        });
        int most = configuration.mostHabits();
        if (own.size() > most) {
            throw CadProblem.invalid("A reader keeps at most " + most + " habits; an interface naming more names something it should not.");
        }
        try {
            settings.change(SECTION.plugin(), SECTION.pathInOwner(), reader, rows);
        } catch (SettingRefused refused) {
            throw CadProblem.invalid(refused.getMessage());
        }
        return habits();
    }

    /** A habit's row: {@code habits[pcb.look]}. */
    private static String keyOf(String habit) {
        return ParameterTree.keyOf(java.util.List.of(new ParameterTree.Segment(HABITS, false), new ParameterTree.Segment(habit, false)));
    }

    private SettingHolder reader() {
        return SettingHolder.reader(READER, authority.requireActor());
    }
}
