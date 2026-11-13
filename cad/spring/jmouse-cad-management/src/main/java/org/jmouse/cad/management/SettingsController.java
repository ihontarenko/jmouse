package org.jmouse.cad.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.settings.SettingsSchemas;
import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.store.plugin.EnginePluginSettings;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.cad.store.settings.SettingsTree;
import org.jmouse.settings.ParameterTree;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 🎚️ The engine's settings tree (ADR 0004, 0007): every section by subject — {@code pcb/layout}, {@code pcb/physics/copper},
 * {@code plugin/cad-gerber/fabrication} —, its values as the installation keeps them, one row a value, and what each means. What an
 * interface draws or computes with comes from here, never from a copy of its own. Every value is the installation's to change — a
 * curator's, as every setting of the engine is —, key by key, and to bring back to what its module ships.
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX)
public class SettingsController {

    private final SettingsTree         tree;
    private final EnginePluginSettings settings;

    public SettingsController(SettingsTree tree, EnginePluginSettings settings) {
        this.tree = tree;
        this.settings = settings;
    }

    /**
     * A section of the tree: where it stands, what it is called — and, asked for, its values as its record reads them, what its
     * module ships read the same way (what a value goes back to), its rows (one a value, by key), the schema written from its
     * record — each value's label, meaning, kind and bounds —, and whether the reader may change it.
     */
    public record SectionView(String path, String title, Object values, Object shipped, Map<String, String> parameters,
                              Object schema, boolean changeable) {

        static SectionView listed(SettingsSection<?> section) {
            return new SectionView(section.path(), section.title(), null, null, null, null, false);
        }

        static SectionView read(SettingsSection<?> section, SettingsTree tree, boolean changeable) {
            return new SectionView(section.path(), section.title(), tree.valuesOf(section), tree.shippedOf(section), tree.parametersOf(section),
                                   SettingsSchemas.of(section), changeable);
        }
    }

    /** Every section of the tree, by path, without its values. */
    @GetMapping("/settings")
    @PublicEndpoint("the engine's settings hold nothing secret; whether the reader may change them is said with each")
    public List<SectionView> sections() {
        return tree.sections().stream().map(SectionView::listed).toList();
    }

    /**
     * A section with its values, or every section of a branch — {@code /settings/pcb} answers {@code pcb/layout},
     * {@code pcb/routing}, {@code pcb/physics/copper} … —; none at the path, not found.
     */
    @GetMapping("/settings/{*path}")
    @PublicEndpoint("the engine's settings hold nothing secret; whether the reader may change them is said with each")
    public List<SectionView> branch(@PathVariable String path) {
        String asked = pathOf(path);
        boolean changeable = settings.changeable();
        List<SectionView> found = tree.sections().stream()
                .filter(section -> section.path().equals(asked) || section.path().startsWith(asked + "/"))
                .map(section -> SectionView.read(section, tree, changeable)).toList();
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
    @PublicEndpoint("the change decides inside its service: setting the engine up is a curator's, installation-wide")
    public SectionView change(@PathVariable String path, @RequestBody Map<String, String> values) {
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
        SettingsSection<?> section = requireSection(path);
        settings.replaceSection(section.plugin(), section.pathInOwner(), ParameterTree.unfold(values));
        return SectionView.read(section, tree, settings.changeable());
    }

    /** Brings a section back to what its module ships. */
    @DeleteMapping("/settings/{*path}")
    @PublicEndpoint("the change decides inside its service: setting the engine up is a curator's, installation-wide")
    public SectionView restore(@PathVariable String path) {
        SettingsSection<?> section = requireSection(path);
        settings.restoreSection(section.plugin(), section.pathInOwner());
        return SectionView.read(section, tree, settings.changeable());
    }

    private SettingsSection<?> requireSection(String path) {
        String asked = pathOf(path);
        return tree.section(asked).orElseThrow(() -> CadProblem.notFound("settings " + asked));
    }

    private static String pathOf(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }
}
