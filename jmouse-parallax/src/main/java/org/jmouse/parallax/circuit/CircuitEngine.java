package org.jmouse.parallax.circuit;

import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.object.CompiledObject;

/**
 * What computes a circuit's batch analyses: the operating point, the transient, the frequency response, the DC sweep
 * and the noise, each answered as the dataset its representation draws — a node's voltage named for the node ({@code
 * out}, V), a part's current for the part ({@code R1}, A), a node's gain and phase as {@code out gain} [dB] and {@code
 * out phase} [°], its noise as {@code out noise} [V/√Hz].
 *
 * <p>Two engines answer: this library's own ({@link ParallaxEngine}), and SPICE, the second one, which a host supplies
 * with a simulator to run ({@code org.jmouse.parallax.spice.SpiceEngine}). They answer the same questions in the same
 * shapes, so either can be drawn, compared or checked. The live bench is the library's alone; this port is the batch
 * analyses only.</p>
 */
public interface CircuitEngine {

    /** Which engine this is: what a computation's {@code engine} setting names it by. */
    CircuitEngineKind kind();

    /** The DC state: node voltages, the parts' currents, the power the parts take, as {@link Dataset.Measurements}. */
    Dataset operatingPoint(CompiledObject circuit);

    /** Every node voltage and the parts' currents from rest over the circuit's time span. */
    Dataset transientResponse(CompiledObject circuit, TransientRequest request, ComputationProgress progress);

    /** Every node's gain and phase over log-spaced frequencies, the circuit linearized at its operating point. */
    Dataset frequencyResponse(CompiledObject circuit, FrequencyRange range, ComputationProgress progress);

    /** Every node's noise density over log-spaced frequencies, the sources uncorrelated. */
    Dataset noiseResponse(CompiledObject circuit, FrequencyRange range, ComputationProgress progress);

    /** The operating point as one source's value, or one of the circuit's parameters, is swept. */
    Dataset directCurrentSweep(CompiledObject circuit, SweepRequest request, ComputationProgress progress);

    /**
     * Log-spaced frequencies from one end to the other.
     *
     * @param points how many frequencies, the two ends among them
     */
    record FrequencyRange(double from, double to, int points) {

        public FrequencyRange {
            double lowest = Math.min(from, to);
            double highest = Math.max(from, to);
            if (!(lowest < highest) || !(lowest > 0)) {
                throw new IllegalArgumentException("The frequency range needs two different ends above zero.");
            }
            from = lowest;
            to = highest;
        }

        /** The frequency of point {@code index} of {@code points}, evenly spaced in its logarithm. */
        public double frequency(int index) {
            return from * Math.pow(10, Math.log10(to / from) * index / (points - 1));
        }
    }

    /**
     * What a transient is asked for.
     *
     * @param samples   how many samples across the run (or across the window)
     * @param everyPart every part's current and every model pin's, for a sheet that draws them all
     * @param levels    the digital nodes' logic levels instead of the analog columns
     * @param window    from and to, seconds: only this stretch, sampled finely; null for the whole run
     */
    record TransientRequest(int samples, boolean everyPart, boolean levels, double[] window) {

        public TransientRequest {
            window = window == null ? null : window.clone();
        }

        @Override
        public double[] window() {
            return window == null ? null : window.clone();
        }
    }

    /**
     * What a DC sweep is asked for: a source of the circuit, or one of its parameters, from one value to another.
     *
     * @param source    the source swept, empty for the first one; ignored when a parameter is named
     * @param parameter the parameter swept instead, empty for none
     */
    record SweepRequest(String source, String parameter, double from, double to, int points) {

        public SweepRequest {
            source = source == null ? "" : source.strip();
            parameter = parameter == null ? "" : parameter.strip();
        }

        /** Whether a parameter is swept rather than a source. */
        public boolean sweepsParameter() {
            return !parameter.isEmpty();
        }

        /** The value at point {@code index}, evenly spaced from one end to the other. */
        public double value(int index) {
            return from + (to - from) * index / (points - 1);
        }
    }
}
