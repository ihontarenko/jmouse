package org.jmouse.cad.plugin.pcbway;

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
 * PCBWay as a board house: the Gerber set with its assembly tables in PCBWay's columns, by the profile in
 * this jar ({@code pcbway.json}). It stands on the Gerber plugin; with that one off, this one is refused too.
 */
public final class PcbwayPlugin implements Plugin {

    public static final String ID = "cad-pcbway";

    /** The format's word on the exchange routes: {@code /exchange/pcbway/export}. */
    static final String FORMAT = "pcbway";

    private final GerberSet set;

    public PcbwayPlugin(GerberSet set) {
        this.set = set;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "PCBWay", PcbwayPlugin.class)
                .drawnBy(PcbwayPlugin.class, "pcbway.svg").requiring(Set.of(GerberPlugin.ID));
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(DocumentExport.POINT, new FabricationExport(FORMAT, "PCBWay", set,
                AssemblyProfile.of(PcbwayPlugin.class, "pcbway.json")));
    }
}
