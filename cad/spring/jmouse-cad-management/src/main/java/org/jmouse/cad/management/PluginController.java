package org.jmouse.cad.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.plugin.Contributions;
import org.jmouse.plugin.Decorations;
import org.jmouse.plugin.ExtensionPoint;
import org.jmouse.plugin.Extensions;
import org.jmouse.plugin.PluginState;
import org.jmouse.plugin.Topic;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 🧩 Which plugins this application runs — {@code JMOUSE_CAD_API.md} §2.8: an interface switches its own halves on by
 * this list, and a plugin left off says why.
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX)
public class PluginController {

    private final Extensions extensions;

    public PluginController(Extensions extensions) {
        this.extensions = extensions;
    }

    /**
     * A plugin as the list shows it.
     *
     * @param reason why it is not running; null for one that is
     */
    public record PluginView(String id, String title, String version, PluginState.Status status, String reason) {
    }

    /** A point the running plugins opened, by name and what it holds. */
    public record PointView(String name, String kind, String holds) {
    }

    public record Plugins(List<PluginView> plugins, List<PointView> points) {
    }

    @GetMapping("/plugins")
    @PublicEndpoint("which plugins run; nothing stored is read")
    public Plugins plugins() {
        return new Plugins(extensions.plugins().stream().map(PluginController::viewOf).toList(),
                           extensions.points().stream().map(PluginController::viewOf).toList());
    }

    private static PluginView viewOf(PluginState state) {
        return new PluginView(state.plugin().id(), state.plugin().title(), state.plugin().version(), state.status(),
                              state.reason());
    }

    private static PointView viewOf(ExtensionPoint<?> point) {
        String kind = switch (point) {
            case Contributions<?> ignored -> "contributions";
            case Decorations<?> ignored -> "decorations";
            case Topic<?> ignored -> "topic";
        };
        return new PointView(point.name(), kind, point.type().getSimpleName());
    }
}
