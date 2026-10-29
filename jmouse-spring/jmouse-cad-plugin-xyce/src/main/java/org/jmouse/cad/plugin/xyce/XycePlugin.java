package org.jmouse.cad.plugin.xyce;

import org.jmouse.cad.simulation.AnalysisEngine;
import org.jmouse.cad.spice.SpiceEngine;
import org.jmouse.cad.spice.SpiceSimulator;
import org.jmouse.cad.spice.XyceDialect;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

import java.util.Optional;

/**
 * Xyce beside the engine's own: the circuit written as the SPICE deck every program reads, closed in Xyce's dialect, run
 * in batch. It brings one engine to the core's point; without this plugin the core simply offers no Xyce.
 */
public final class XycePlugin implements Plugin {

    public static final String ID = "cad-xyce";

    /** The engine's word on the routes: {@code /simulations/engines/xyce/transient}. */
    static final String ENGINE = "xyce";

    /** The property that names the program, said when it is off. */
    static final String SETTING = "jmouse.cad.xyce.command";

    private final Optional<SpiceSimulator> simulator;

    /** @param simulator Xyce as a process; empty for a server without it, whose engine is then listed as off */
    public XycePlugin(Optional<SpiceSimulator> simulator) {
        this.simulator = simulator;
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "Xyce", XycePlugin.class).drawnBy(XycePlugin.class, "xyce.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(AnalysisEngine.POINT, new SpiceEngine(ENGINE, new XyceDialect(), simulator, SETTING));
    }
}
