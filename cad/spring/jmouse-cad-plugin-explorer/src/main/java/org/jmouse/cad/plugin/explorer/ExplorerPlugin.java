package org.jmouse.cad.plugin.explorer;

import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

import java.util.List;

/**
 * The explorer as a plugin: it opens {@link ExplorerMount#POINT}, where the server's own folders are brought and any
 * other plugin may mount a store, and {@link ExplorerChange#TOPIC}, where every change it makes is told — so a product's
 * index of the files (the CAD engine's documents) follows them. What opens a file is the interface's.
 */
public final class ExplorerPlugin implements Plugin {

    public static final String ID = "cad-explorer";

    private final List<ExplorerMount> mounted;

    /** @param mounted the server's folders, as its settings name them */
    public ExplorerPlugin(List<ExplorerMount> mounted) {
        this.mounted = List.copyOf(mounted);
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "Explorer", ExplorerPlugin.class).drawnBy(ExplorerPlugin.class, "explorer.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.declare(ExplorerMount.POINT);
        registration.declare(ExplorerChange.TOPIC);
        mounted.forEach(mount -> registration.contribute(ExplorerMount.POINT, mount));
    }
}
