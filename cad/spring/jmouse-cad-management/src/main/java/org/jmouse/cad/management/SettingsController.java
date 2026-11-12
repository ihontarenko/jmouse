package org.jmouse.cad.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.cad.store.settings.SettingsTree;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 🎚️ The engine's settings tree (ADR 0004): every section by subject — {@code pcb/layout}, {@code pcb/physics/copper},
 * {@code plugin/gerber/fabrication} —, its owner and its values. What an interface draws or computes with comes from here, never
 * from a copy of its own. The engine's shipped data, the same for every reader.
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX)
public class SettingsController {

    private final SettingsTree tree;

    public SettingsController(SettingsTree tree) {
        this.tree = tree;
    }

    /** A section of the tree: where it stands, what it is called, who owns it — and, asked for, its values. */
    public record SectionView(String path, String title, String owner, Object values) {

        static SectionView listed(SettingsSection<?> section) {
            return new SectionView(section.path(), section.title(), section.owner().word(), null);
        }

        static SectionView read(SettingsSection<?> section, SettingsTree tree) {
            return new SectionView(section.path(), section.title(), section.owner().word(), tree.valuesOf(section));
        }
    }

    /** Every section of the tree, by path, without its values. */
    @GetMapping("/settings")
    @PublicEndpoint("the engine's shipped settings, the same for every reader")
    public List<SectionView> sections() {
        return tree.sections().stream().map(SectionView::listed).toList();
    }

    /**
     * A section with its values, or every section of a branch — {@code /settings/pcb} answers {@code pcb/layout},
     * {@code pcb/routing}, {@code pcb/physics/copper} … —; none at the path, not found.
     */
    @GetMapping("/settings/{*path}")
    @PublicEndpoint("the engine's shipped settings, the same for every reader")
    public List<SectionView> branch(@PathVariable String path) {
        String asked = path.startsWith("/") ? path.substring(1) : path;
        List<SectionView> found = tree.sections().stream()
                .filter(section -> section.path().equals(asked) || section.path().startsWith(asked + "/"))
                .map(section -> SectionView.read(section, tree)).toList();
        if (found.isEmpty()) {
            throw CadProblem.notFound("settings " + asked);
        }
        return found;
    }
}
