package org.jmouse.cad.simulation.engine;

import java.util.Map;

/**
 * One sample of a running circuit: when, every node's voltage then, in the order its solver reports its nodes, and —
 * on a traced sample — the current into each part's terminals, by part and terminal; null on any other.
 *
 * @param metered  the metered parts' readings at the sample, by part; null where nothing is metered
 * @param window   what a traced sample stands for when the trace keeps one sample in many; null otherwise
 * @param envelope the peaks and means since the sample before; null where the readings are not kept
 * @param powers   where the sample traced its parts: each traced part's power at the sample, watts, by the engine's
 *                 definition ({@link Device#power}) — what a reader of the trace shows, never works out itself
 * @param levels   every digital node's level at the sample, by node — what a logic analyser's channel reads; null where
 *                 the readings are not kept or the circuit has no digital node
 */
public record LiveSample(double time, double[] voltages, Map<String, Map<String, Double>> terminals,
                         Map<String, MeteredPart> metered, SampleWindow window, SampleEnvelope envelope,
                         Map<String, Double> powers, Map<String, LogicLevel> levels) {

    public LiveSample(double time, double[] voltages, Map<String, Map<String, Double>> terminals,
                      Map<String, MeteredPart> metered, SampleWindow window, SampleEnvelope envelope,
                      Map<String, Double> powers) {
        this(time, voltages, terminals, metered, window, envelope, powers, null);
    }

    public LiveSample(double time, double[] voltages, Map<String, Map<String, Double>> terminals, Map<String, MeteredPart> metered) {
        this(time, voltages, terminals, metered, null, null, null, null);
    }
}
