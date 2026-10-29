package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

/**
 * Gerber in the engine: a board written as the files a board house makes and assembles it from. It brings the plain set
 * to the core's exports; a board house's plugin requires it and builds its own set on {@link BoardFabrication},
 * {@link GerberSet} and {@link FabricationExport} with its own {@link AssemblyProfile}. Without it the core writes no board.
 */
public final class GerberPlugin implements Plugin {

    public static final String ID = "cad-gerber";

    /** The format's word on the exchange routes: {@code /exchange/gerber/export}. */
    static final String FORMAT = "gerber";

    private final GerberSet set;

    public GerberPlugin(GerberSet set) {
        this.set = set;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "Gerber", GerberPlugin.class).drawnBy(GerberPlugin.class, "gerber.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(DocumentExport.POINT, new FabricationExport(FORMAT, "Gerber X2 · Excellon", set,
                AssemblyProfile.of(GerberPlugin.class, "assembly.json")));
    }
}
