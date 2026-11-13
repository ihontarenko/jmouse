package org.jmouse.cad.plugin.xyce;

import org.jmouse.cad.table.ShippedTable;
import org.jmouse.cad.spice.SpiceDevices;
import org.jmouse.cad.table.ReadTable;
import org.jmouse.settings.Section;
import org.jmouse.cad.simulation.AnalysisEngine;
import org.jmouse.cad.spice.SpiceEngine;
import org.jmouse.cad.spice.SpiceProcess;
import org.jmouse.cad.spice.SpiceSimulator;
import org.jmouse.cad.spice.XyceDialect;
import org.jmouse.plugin.Plugin;
import org.jmouse.plugin.PluginDescriptor;
import org.jmouse.plugin.PluginRegistration;

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
    /** The devices SPICE knows, as the engine's table says it at each use. */
    private final ReadTable<SpiceDevices> devices;
    /** How the program is started while nobody changed it: what the server is configured with. */
    private final List<String>   usualCommand;
    /** How long a run may take while nobody changed it: what the server is configured with. */
    private final Duration       usualTimeout;

    /** @param simulator Xyce as a process, its engine listed as off while its setting names no program */
    public XycePlugin(SpiceSimulator simulator, List<String> usualCommand, Duration usualTimeout, ReadTable<SpiceDevices> devices) {
        this.devices = devices;
        this.simulator = simulator;
        this.usualCommand = List.copyOf(usualCommand);
        this.usualTimeout = usualTimeout;
    }

    @Override
    public List<Section> settings() {
        return List.of(SpiceProcess.programSection("Xyce", "the run's CSV", usualCommand, usualTimeout));
    }

    @Override
    public PluginDescriptor descriptor() {
        return PluginDescriptor.ofArtifact(ID, "Xyce", XycePlugin.class).markedBy(XycePlugin.class, "xyce.svg");
    }

    @Override
    public void register(PluginRegistration registration) {
        // The tables its module reads, as pages of the library.
        registration.contribute(ShippedTable.POINT, SpiceDevices.SHIPPED.shipping(SpiceDevices.TABLE));
        registration.contribute(AnalysisEngine.POINT, new SpiceEngine(ENGINE, new XyceDialect(), simulator, devices));
    }
}
