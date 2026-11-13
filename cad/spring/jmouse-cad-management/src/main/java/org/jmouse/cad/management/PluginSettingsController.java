package org.jmouse.cad.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.store.plugin.EnginePluginSettings;
import org.jmouse.plugin.ServerFolders;
import org.jmouse.cad.store.plugin.EnginePluginSettings.EngineSettingsView;
import org.jmouse.cad.store.plugin.EnginePluginSettings.PluginSettingsView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 🎚️ The settings of the plugins the engine runs: what each may be set to and what it stands at, and the change a
 * curator makes, installation-wide. A plugin reads its settings each time it is used, so a change applies at once. They
 * are not under {@code /plugins/{id}}, which is the plugins' own routes: a plugin may name one of its routes as it likes.
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX)
public class PluginSettingsController {

    private final EnginePluginSettings settings;

    public PluginSettingsController(EnginePluginSettings settings) {
        this.settings = settings;
    }

    @GetMapping("/plugin-settings")
    @PublicEndpoint("a plugin's settings hold nothing secret; whether the reader may change them is said with them")
    public EngineSettingsView settings() {
        return settings.settings();
    }

    /** The drives of the server, to choose a folder from. */
    @GetMapping("/plugin-settings/server-folders/roots")
    @PublicEndpoint("decides inside its service: only who may set the engine up is shown the server's folders")
    public List<ServerFolders.Folder> serverRoots() {
        return settings.serverRoots();
    }

    /** What a folder of the server holds: only folders, to choose one from. */
    @GetMapping("/plugin-settings/server-folders")
    @PublicEndpoint("decides inside its service: only who may set the engine up is shown the server's folders")
    public ServerFolders.Listing serverFolder(@RequestParam String path) {
        return settings.serverFolder(path);
    }

    /** Changes one section of a plugin's parameters, all or none; a null value brings a parameter back to its default. */
    @PutMapping("/plugin-settings/{plugin}/{*section}")
    @PublicEndpoint("the change decides inside its service: setting the engine up is a curator's, installation-wide")
    public PluginSettingsView change(@PathVariable String plugin, @PathVariable String section, @RequestBody Map<String, String> values) {
        return settings.change(plugin, section.startsWith("/") ? section.substring(1) : section, values);
    }
}
