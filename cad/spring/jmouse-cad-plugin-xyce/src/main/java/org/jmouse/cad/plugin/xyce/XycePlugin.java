package org.jmouse.cad.plugin.xyce;

import org.jmouse.cad.simulation.AnalysisEngine;
import org.jmouse.cad.spice.SpiceEngine;
import org.jmouse.cad.spice.SpiceProcess;
import org.jmouse.cad.spice.SpiceSimulator;
import org.jmouse.cad.spice.XyceDialect;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;
import org.jmouse.plugin.PluginSetting;

import java.time.Duration;
import java.util.List;

/**
 * Xyce beside the engine's own: the circuit written as the SPICE deck every program reads, closed in Xyce's dialect, run
 * in batch. It brings one engine to the core's point; without this plugin the core simply offers no Xyce.
 */
public final class XycePlugin implements Plugin {

    public static final String ID = "cad-xyce";

    /** The engine's word on the routes: {@code /simulations/engines/xyce/transient}. */
    static final String ENGINE = "xyce";

    private final SpiceSimulator simulator;
    /** How the program is started while nobody changed it: what the server is configured with. */
    private final List<String>   usualCommand;
    /** How long a run may take while nobody changed it: what the server is configured with. */
    private final Duration       usualTimeout;

    /** @param simulator Xyce as a process, its engine listed as off while its setting names no program */
    public XycePlugin(SpiceSimulator simulator, List<String> usualCommand, Duration usualTimeout) {
        this.simulator = simulator;
        this.usualCommand = List.copyOf(usualCommand);
        this.usualTimeout = usualTimeout;
    }

    @Override
    public List<PluginSetting> settings() {
        return List.of(SpiceProcess.commandSetting("Xyce", "the run's CSV", usualCommand),
                       SpiceProcess.timeoutSetting("Xyce", usualTimeout));
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "Xyce", XycePlugin.class).markedBy(XycePlugin.class, "xyce.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        registration.contribute(AnalysisEngine.POINT, new SpiceEngine(ENGINE, new XyceDialect(), simulator));
    }
}
