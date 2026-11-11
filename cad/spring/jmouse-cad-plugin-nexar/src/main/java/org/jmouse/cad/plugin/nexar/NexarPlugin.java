package org.jmouse.cad.plugin.nexar;

import org.jmouse.cad.document.part.PartSearchProvider;
import org.jmouse.cad.nexar.NexarParts;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

/** Nexar's supply search (Octopart's data) as a place parts are looked for. */
public final class NexarPlugin implements Plugin {

    public static final String ID = "cad-nexar";

    private final NexarParts parts;

    public NexarPlugin(NexarParts parts) {
        this.parts = parts;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "Nexar", NexarPlugin.class).drawnBy(NexarPlugin.class, "nexar.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(PartSearchProvider.POINT, parts);
    }
}
