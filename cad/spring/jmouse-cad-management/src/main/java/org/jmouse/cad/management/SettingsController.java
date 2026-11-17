package org.jmouse.cad.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.settings.SettingsSchemas;
import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.store.plugin.EnginePluginSettings;
import org.jmouse.cad.store.plugin.EnginePluginSettings.FixedSection;
import org.jmouse.cad.store.plugin.EnginePluginSettings.ParameterView;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.cad.store.settings.ReaderSettings;
import org.jmouse.cad.store.settings.SettingsTree;
import org.jmouse.settings.ParameterTree;
import org.jmouse.settings.SettingRefused;
import org.jmouse.settings.Settings;
import org.jmouse.settings.SettingsFile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * 🎚️ The one resource of settings (ADR 0004, 0007; plan M): every section by its path, read, changed and brought back the same way,
 * whoever holds it and whatever its shape ({@link Shape}). The engine's and its plugins' sections — a record, {@code pcb/layout},
 * {@code plugin/cad-gerber/fabrication}; fixed parameters, {@code part-search}, {@code plugin/cad-suppliers/mouser} — are the
 * installation's, changed by whoever sets the engine up; the interface's — {@code interface/editor/grid}, {@code interface/drawing} —
 * are each reader's own rows, which the engine holds to their form and knows nothing more of ({@link ReaderSettings}). Whose a change
 * is the path says, and {@code ?holder=reader|installation} may say it outright; a holder the section is not held by is refused.
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX)
public class SettingsController {

    /** What a file of settings is called when it is saved: `engine-settings-2026-11-14.settings`. */
    private static final String FILE_NAME = "engine-settings-%s.settings";

    private final SettingsTree         tree;
    private final EnginePluginSettings settings;
    private final Settings             store;
    private final ReaderSettings       reader;

    public SettingsController(SettingsTree tree, EnginePluginSettings settings, Settings store, ReaderSettings reader) {
        this.tree = tree;
        this.settings = settings;
        this.store = store;
        this.reader = reader;
    }

    /**
     * The signed-in reader's settings as a file ({@link SettingsFile}): the rows they keep, and — for whoever may set the engine up —
     * every section the installation changed. Kept, carried, and taken in again here or on another installation.
     */
    @GetMapping("/settings-file")
    @PublicEndpoint("decides inside its service: a reader's own rows, and the installation's only to whoever may set the engine up")
    public FileView file() {
        LocalDate today = LocalDate.now();
        return new FileView(FILE_NAME.formatted(today),
                            SettingsFile.write("Engine settings · " + today, store.fileOf(reader.reader(), settings.changeable())));
    }

    /** A file of settings: what it is called when saved, and its text. */
    public record FileView(String name, String text) {
    }

    /** A file's text, as it was chosen. */
    public record FileText(String text) {
    }

    /**
     * Takes in a file of settings: its reader's rows as the signed-in reader's, the installation's sections only for whoever may set the
     * engine up — each block on its own, a refused one named with why. A file that does not read is refused whole.
     */
    @PostMapping("/settings-file")
    @PublicEndpoint("decides inside its service: a reader's own rows, and the installation's only to whoever may set the engine up")
    public Settings.Taken take(@RequestBody FileText file) {
        try {
            return store.take(SettingsFile.read(file.text()), reader.reader(), settings.changeable());
        } catch (SettingRefused unread) {
            throw CadProblem.invalid(unread.getMessage());
        }
    }

    /**
     * What a section is made of, and so how it is read and changed.
     *
     * <ul>
     *   <li>{@link #RECORD} — a record a module ships, read into its type: its values, what ships, its rows, its schema.</li>
     *   <li>{@link #PARAMETERS} — fixed parameters a plugin declares, each with its label, kind and bounds: changed value by value,
     *       a key to a service never told back.</li>
     *   <li>{@link #OWN} — a reader's own rows of the interface's, which the engine holds to their form and knows nothing more of.</li>
     * </ul>
     */
    public enum Shape {
        RECORD,
        PARAMETERS,
        OWN
    }

    /**
     * A section of the tree: where it stands, what it is called, what it is made of — and, asked for, its values as its record reads
     * them, what its module ships read the same way (what a value goes back to), its rows (one a value, by key), the schema written
     * from its record — each value's label, meaning, kind and bounds —, a section of parameters its parameters as declared, and
     * whether the reader may change it. Listed or read, it says whether the installation changed any of it from what ships: what a
     * window marks before the section is opened.
     */
    public record SectionView(String path, String title, Shape shape, String description, Object values, Object shipped,
                              Map<String, String> parameters, Object schema, List<ParameterView> declared, boolean changeable,
                              boolean changed) {

        static SectionView listed(SettingsSection<?> section, SettingsTree tree) {
            return new SectionView(section.path(), section.title(), Shape.RECORD, null, null, null, null, null, null, false,
                                   differs(tree.valuesOf(section), tree.shippedOf(section)));
        }

        /** A section of the interface's: the reader's own rows of it, the only values the engine has of it. */
        static SectionView own(String path, Map<String, String> rows) {
            return new SectionView(path, path, Shape.OWN, null, rows, null, rows, null, null, true, !rows.isEmpty());
        }

        static SectionView read(SettingsSection<?> section, SettingsTree tree, boolean changeable) {
            Object values = tree.valuesOf(section);
            Object shipped = tree.shippedOf(section);
            return new SectionView(section.path(), section.title(), Shape.RECORD, null, values, shipped, tree.parametersOf(section),
                                   SettingsSchemas.of(section), null, changeable, differs(values, shipped));
        }

        static SectionView read(FixedSection section, EnginePluginSettings settings) {
            return new SectionView(section.path(), section.label(), Shape.PARAMETERS, section.description(), null, null, null, null,
                                   settings.parametersOf(section), settings.changeable(), settings.changed(section));
        }

        /** Whether a section's values are other than what ships: records compare by their values, lists by their records. */
        private static boolean differs(Object values, Object shipped) {
            return !Objects.deepEquals(values, shipped);
        }
    }

    /**
     * Every section of the tree, by path: what each is made of, whether it was changed from what ships — a record without its values,
     * a section of parameters with them, small as they are, so a window says which plugin waits for a key before it is opened.
     */
    @GetMapping("/settings")
    @PublicEndpoint("the engine's settings hold nothing secret; whether the reader may change them is said with each")
    public List<SectionView> sections() {
        return Stream.concat(tree.sections().stream().map(section -> SectionView.listed(section, tree)),
                             settings.fixedSections().stream().map(section -> SectionView.read(section, settings)))
                .sorted(Comparator.comparing(SectionView::path)).toList();
    }

    /**
     * A section with its values, or every section of a branch — {@code /settings/pcb} answers {@code pcb/layout},
     * {@code pcb/routing}, {@code pcb/physics/copper} … —; none at the path, not found.
     */
    @GetMapping("/settings/{*path}")
    @PublicEndpoint("the engine's settings hold nothing secret; whether the reader may change them is said with each")
    public List<SectionView> branch(@PathVariable String path) {
        String asked = pathOf(path);
        if (ReaderSettings.covers(asked)) {
            // The interface's: every section at or below the path the reader keeps rows of — the one asked even with none.
            Map<String, Map<String, String>> under = new TreeMap<>(reader.rowsUnder(asked));
            if (!asked.equals(ReaderSettings.BRANCH)) {
                under.putIfAbsent(asked, Map.of());
            }
            return under.entrySet().stream().map(section -> SectionView.own(section.getKey(), section.getValue())).toList();
        }
        boolean changeable = settings.changeable();
        List<SectionView> found = Stream.concat(
                        tree.sections().stream().filter(section -> within(section.path(), asked)).map(section -> SectionView.read(section, tree, changeable)),
                        settings.fixedSections().stream().filter(section -> within(section.path(), asked)).map(section -> SectionView.read(section, settings)))
                .sorted(Comparator.comparing(SectionView::path)).toList();
        if (found.isEmpty()) {
            throw CadProblem.notFound("settings " + asked);
        }
        return found;
    }

    /**
     * Changes values of a section, key by key — {@code "circuit.gap": "2"} —, checked by reading the section whole into its record
     * before anything is kept; a null value brings its key back to what ships, and a key the section ships none of is taken away.
     */
    @PatchMapping("/settings/{*path}")
    @PublicEndpoint("the change decides inside its service: a reader changes their own rows, setting the engine up is a curator's")
    public SectionView change(@PathVariable String path, @RequestParam(required = false) String holder, @RequestBody Map<String, String> values) {
        String asked = pathOf(path);
        if (heldBy(asked, holder) == SettingsFile.Holding.READER) {
            return SectionView.own(asked, reader.change(asked, values));
        }
        Optional<FixedSection> fixed = settings.fixedSection(asked);
        if (fixed.isPresent()) {
            settings.changeSection(fixed.get().plugin(), fixed.get().pathInOwner(), values);
            return SectionView.read(fixed.get(), settings);
        }
        SettingsSection<?> section = requireSection(path);
        settings.changeSection(section.plugin(), section.pathInOwner(), values);
        return SectionView.read(section, tree, settings.changeable());
    }

    /**
     * Replaces a section with a whole record — an editor's, its lists grown or shrunk —: unfolded into its rows here, one a value,
     * and read into its record before anything is kept.
     */
    @PutMapping("/settings/{*path}")
    @PublicEndpoint("the change decides inside its service: setting the engine up is a curator's, installation-wide")
    public SectionView replace(@PathVariable String path, @RequestBody Object values) {
        if (settings.fixedSection(pathOf(path)).isPresent()) {
            throw CadProblem.invalid("Settings " + pathOf(path) + " are parameters, changed value by value: there is no record to replace.");
        }
        SettingsSection<?> section = requireSection(path);
        settings.replaceSection(section.plugin(), section.pathInOwner(), ParameterTree.unfold(values));
        return SectionView.read(section, tree, settings.changeable());
    }

    /** Brings a section back to what its module ships. */
    @DeleteMapping("/settings/{*path}")
    @PublicEndpoint("the change decides inside its service: a reader forgets their own rows, setting the engine up is a curator's")
    public SectionView restore(@PathVariable String path, @RequestParam(required = false) String holder) {
        String asked = pathOf(path);
        if (heldBy(asked, holder) == SettingsFile.Holding.READER) {
            reader.forget(asked);
            return SectionView.own(asked, Map.of());
        }
        Optional<FixedSection> fixed = settings.fixedSection(asked);
        if (fixed.isPresent()) {
            settings.restoreSection(fixed.get().plugin(), fixed.get().pathInOwner());
            return SectionView.read(fixed.get(), settings);
        }
        SettingsSection<?> section = requireSection(path);
        settings.restoreSection(section.plugin(), section.pathInOwner());
        return SectionView.read(section, tree, settings.changeable());
    }

    /**
     * Who holds a section's rows a change is made to: the reader for the interface's, the installation for the engine's — said by the
     * path, or by `holder`, which must agree with it.
     */
    private static SettingsFile.Holding heldBy(String path, String holder) {
        SettingsFile.Holding held = ReaderSettings.covers(path) ? SettingsFile.Holding.READER : SettingsFile.Holding.INSTALLATION;
        if (holder != null && !holder.isBlank() && !holder.equals(held.word())) {
            throw CadProblem.invalid("Settings " + path + " are held by " + (held == SettingsFile.Holding.READER ? "each reader" : "the installation")
                                     + ", not by '" + holder + "'.");
        }
        return held;
    }

    private SettingsSection<?> requireSection(String path) {
        String asked = pathOf(path);
        return tree.section(asked).orElseThrow(() -> CadProblem.notFound("settings " + asked));
    }

    /** Whether a section's path is the one asked, or stands below it. */
    private static boolean within(String path, String asked) {
        return path.equals(asked) || path.startsWith(asked + "/");
    }

    private static String pathOf(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }
}
