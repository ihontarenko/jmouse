package org.jmouse.cad.simulation.engine;

import java.util.Map;

/**
 * What a traced sample stands for when the trace keeps one sample in many: the stretch since the last traced one —
 * every node's mean voltage and RMS over it, in the order the solver reports its nodes, and every terminal's RMS
 * current; the sample's {@code terminals} are then the mean currents.
 */
public record SampleWindow(double[] means, double[] rms, Map<String, Map<String, Double>> terminalRms) {
}
