package org.jmouse.cad.plugin.spice;

import org.jmouse.cad.exchange.DocumentExport;
import org.jmouse.cad.exchange.DocumentImport;
import org.jmouse.cad.simulation.AnalysisEngine;
import org.jmouse.cad.spice.NgspiceDialect;
import org.jmouse.cad.spice.SpiceEngine;
import org.jmouse.cad.spice.SpiceSimulator;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

import java.util.List;
import java.util.Optional;

/**
 * SPICE in jmCAD: netlists read, decks written, ngspice run beside the engine. All of it is brought to the core's
 * points; without this plugin the core simply offers no SPICE.
 */
public final class SpicePlugin implements Plugin {

    public static final String ID = "cad-spice";

    /** The format's word on the exchange routes: {@code /exchange/spice/read}. */
    static final String FORMAT = "spice";

    static final String TITLE = "SPICE netlist";

    /** The files a netlist and its libraries are kept in. */
    static final List<String> FILE_NAMES = List.of("*.cir", "*.sp", "*.spice", "*.net", "*.lib", "*.sub", "*.txt");

    private final Optional<SpiceSimulator> simulator;

    /** @param simulator ngspice as a process; empty for a server without it, whose engine is then listed as off */
    public SpicePlugin(Optional<SpiceSimulator> simulator) {
        this.simulator = simulator;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "SPICE", SpicePlugin.class).drawnBy(SpicePlugin.class, "spice.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(DocumentImport.POINT, new SpiceNetlistImport());
        registration.contribute(DocumentExport.POINT, new SpiceDeckExport());
        registration.contribute(AnalysisEngine.POINT, new SpiceEngine("ngspice", new NgspiceDialect(), simulator, "jmouse.cad.spice.command"));
    }
}
