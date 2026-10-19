package org.jmouse.parallax.circuit;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The stretch of a live run between two traced samples, as the solver went through it step by step: every node's
 * voltage and every terminal's current, each weighted by the step it held over.
 *
 * <p>A trace that keeps one sample in thousands, read at that one instant, lands on whatever phase a fast signal had
 * then — at 20 kHz a different one every time, and the sheet flickers like a stroboscope. Its mean over the stretch
 * does not, and its RMS keeps what the mean cancels: a current that alternates moves no charge, and still heats.</p>
 *
 * <p>It is taken on every accepted step, so it keeps flat arrays: a terminal is a column, laid out once by the
 * integrator. Maps built on every step were the largest cost of a run at real time.</p>
 */
final class TraceWindow {

    /** A closed stretch: the terminals' mean currents, as a traced sample's terminals, and the rest of the stretch. */
    record Closed(Map<String, Map<String, Double>> terminals, SampleWindow window) {
    }

    private final double[] voltageAreas;
    private final double[] voltageSquares;
    /** Each terminal column's part and terminal name. */
    private final String[] parts;
    private final String[] terminals;
    private final double[] currentAreas;
    private final double[] currentSquares;
    private double         duration;

    TraceWindow(int nodes, String[] parts, String[] terminals) {
        this.voltageAreas = new double[nodes];
        this.voltageSquares = new double[nodes];
        this.parts = parts;
        this.terminals = terminals;
        this.currentAreas = new double[parts.length];
        this.currentSquares = new double[parts.length];
    }

    /** How much circuit time it has taken in since it was last closed. */
    double duration() {
        return duration;
    }

    /** One accepted step: the state at its end and each terminal column's current then, held for {@code step} seconds. */
    void take(double[] state, double[] currents, double step) {
        for (int node = 0; node < voltageAreas.length; node++) {
            double voltage = state[node];
            voltageAreas[node] += voltage * step;
            voltageSquares[node] += voltage * voltage * step;
        }
        for (int column = 0; column < currentAreas.length; column++) {
            double current = currents[column];
            currentAreas[column] += current * step;
            currentSquares[column] += current * current * step;
        }
        duration += step;
    }

    /** The means and RMS of the stretch, and a new stretch begun. */
    Closed close() {
        int nodes = voltageAreas.length;
        double[] means = new double[nodes];
        double[] rms = new double[nodes];
        for (int node = 0; node < nodes; node++) {
            means[node] = voltageAreas[node] / duration;
            rms[node] = Math.sqrt(Math.max(0, voltageSquares[node] / duration));
        }
        Map<String, Map<String, Double>> currentMeans = new LinkedHashMap<>();
        Map<String, Map<String, Double>> currentRms = new LinkedHashMap<>();
        for (int column = 0; column < currentAreas.length; column++) {
            currentMeans.computeIfAbsent(parts[column], ignored -> new LinkedHashMap<>()).put(terminals[column], currentAreas[column] / duration);
            currentRms.computeIfAbsent(parts[column], ignored -> new LinkedHashMap<>())
                    .put(terminals[column], Math.sqrt(Math.max(0, currentSquares[column] / duration)));
        }
        clear();
        return new Closed(currentMeans, new SampleWindow(means, rms, currentRms));
    }

    /** Forgets what it took in: a sample read at its instant stands for itself. */
    void clear() {
        Arrays.fill(voltageAreas, 0);
        Arrays.fill(voltageSquares, 0);
        Arrays.fill(currentAreas, 0);
        Arrays.fill(currentSquares, 0);
        duration = 0;
    }
}
