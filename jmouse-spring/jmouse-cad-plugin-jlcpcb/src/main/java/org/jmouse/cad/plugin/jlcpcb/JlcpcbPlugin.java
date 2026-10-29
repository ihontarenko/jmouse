package org.jmouse.cad.plugin.jlcpcb;

import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.plugin.gerber.AssemblyProfile;
import org.jmouse.cad.plugin.gerber.FabricationExport;
import org.jmouse.cad.plugin.gerber.GerberPlugin;
import org.jmouse.cad.plugin.gerber.GerberSet;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

import java.util.Set;

/**
 * JLCPCB as a board house: the Gerber set with its assembly tables in JLCPCB's columns, by the profile in
 * this jar ({@code jlcpcb.json}). It stands on the Gerber plugin; with that one off, this one is refused too.
 */
public final class JlcpcbPlugin implements Plugin {

    public static final String ID = "cad-jlcpcb";

    /** The format's word on the exchange routes: {@code /exchange/jlcpcb/export}. */
    static final String FORMAT = "jlcpcb";

    private final GerberSet set;

    public JlcpcbPlugin(GerberSet set) {
        this.set = set;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "JLCPCB", JlcpcbPlugin.class)
                .drawnBy(JlcpcbPlugin.class, "jlcpcb.svg").requiring(Set.of(GerberPlugin.ID));
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(DocumentExport.POINT, new FabricationExport(FORMAT, "JLCPCB", set,
                AssemblyProfile.of(JlcpcbPlugin.class, "jlcpcb.json")));
    }
}
