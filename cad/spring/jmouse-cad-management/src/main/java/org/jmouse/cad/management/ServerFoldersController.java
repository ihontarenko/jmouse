package org.jmouse.cad.management;

import org.jmouse.access.enforcement.PublicEndpoint;
import org.jmouse.cad.store.plugin.EnginePluginSettings;
import org.jmouse.plugin.ServerFolders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 📁 The server's folders, to choose one from where a setting names a folder of the server — never a setting itself: the settings
 * are the one resource {@code /settings}. Only whoever may set the engine up is shown them.
 */
@RestController
@RequestMapping(CadManagementRoutes.PREFIX)
public class ServerFoldersController {

    private final EnginePluginSettings settings;

    public ServerFoldersController(EnginePluginSettings settings) {
        this.settings = settings;
    }

    /** The drives of the server, to choose a folder from. */
    @GetMapping("/server-folders/roots")
    @PublicEndpoint("decides inside its service: only who may set the engine up is shown the server's folders")
    public List<ServerFolders.Folder> roots() {
        return settings.serverRoots();
    }

    /** What a folder of the server holds: only folders, to choose one from. */
    @GetMapping("/server-folders")
    @PublicEndpoint("decides inside its service: only who may set the engine up is shown the server's folders")
    public ServerFolders.Listing folder(@RequestParam String path) {
        return settings.serverFolder(path);
    }
}
