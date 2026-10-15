package org.jmouse.parallax.numerics;

import org.jmouse.parallax.ComputationProgress;

import java.util.ArrayList;
import java.util.List;

/**
 * Dormand–Prince 5(4) with adaptive steps: the integrator a Lorenz attractor needs to stay on the attractor, and
 * a harmonic oscillator needs to stay closed. Output is resampled at a fixed interval so a trajectory is a
 * polyline of predictable size.
 */
public final class OdeIntegrator {

    @FunctionalInterface
    public interface OrdinarySystem {
        /** Writes dX/dt at (time, state) into `derivative`. */
        void derivative(double time, double[] state, double[] derivative);
    }

    public record Solution(double[] times, double[][] states, boolean escaped, int steps) {
    }

    private static final double[] C = {0, 1.0 / 5, 3.0 / 10, 4.0 / 5, 8.0 / 9, 1, 1};
    private static final double[][] A = {
            {},
            {1.0 / 5},
            {3.0 / 40, 9.0 / 40},
            {44.0 / 45, -56.0 / 15, 32.0 / 9},
            {19372.0 / 6561, -25360.0 / 2187, 64448.0 / 6561, -212.0 / 729},
            {9017.0 / 3168, -355.0 / 33, 46732.0 / 5247, 49.0 / 176, -5103.0 / 18656},
            {35.0 / 384, 0, 500.0 / 1113, 125.0 / 192, -2187.0 / 6784, 11.0 / 84}
    };
    private static final double[] B5 = {35.0 / 384, 0, 500.0 / 1113, 125.0 / 192, -2187.0 / 6784, 11.0 / 84, 0};
    private static final double[] B4 = {5179.0 / 57600, 0, 7571.0 / 16695, 393.0 / 640, -92097.0 / 339200, 187.0 / 2100, 1.0 / 40};

    private OdeIntegrator() {
    }

    /**
     * Integrates from `initial` over [start, end] (end may be before start: integration runs backwards) and samples
     * the state `samples` times. Stops early when the state leaves `escapeRadius`.
     */
    public static Solution integrate(OrdinarySystem system, double[] initial, double start, double end, int samples,
                                     double tolerance, int maximumSteps, double escapeRadius, ComputationProgress progress) {
        int dimension = initial.length;
        double direction = Math.signum(end - start);
        double span = Math.abs(end - start);
        double[] state = initial.clone();
        double time = start;
        double step = direction * span / 1000;
        double[][] stages = new double[7][dimension];
        double[] temporary = new double[dimension];
        double[] fifth = new double[dimension];
        List<double[]> sampledStates = new ArrayList<>(samples);
        List<Double> sampledTimes = new ArrayList<>(samples);
        sampledStates.add(state.clone());
        sampledTimes.add(time);
        int nextSample = 1;
        int steps = 0;
        boolean escaped = false;
        double[] previous = state.clone();
        double previousTime = time;
        while (direction * (end - time) > 1e-12 && steps < maximumSteps) {
            if (direction * (time + step - end) > 0) {
                step = end - time;
            }
            for (int stage = 0; stage < 7; stage++) {
                for (int component = 0; component < dimension; component++) {
                    double sum = state[component];
                    for (int earlier = 0; earlier < stage; earlier++) {
                        sum += step * A[stage][earlier] * stages[earlier][component];
                    }
                    temporary[component] = sum;
                }
                system.derivative(time + C[stage] * step, temporary, stages[stage]);
            }
            double error = 0;
            for (int component = 0; component < dimension; component++) {
                double high = state[component];
                double low = state[component];
                for (int stage = 0; stage < 7; stage++) {
                    high += step * B5[stage] * stages[stage][component];
                    low += step * B4[stage] * stages[stage][component];
                }
                fifth[component] = high;
                double scale = tolerance * (1 + Math.max(Math.abs(state[component]), Math.abs(high)));
                error = Math.max(error, Math.abs(high - low) / scale);
            }
            steps++;
            if (!Double.isFinite(error)) {
                escaped = true;
                break;
            }
            if (error <= 1) {
                System.arraycopy(state, 0, previous, 0, dimension);
                previousTime = time;
                time += step;
                System.arraycopy(fifth, 0, state, 0, dimension);
                while (nextSample < samples && direction * (time - sampleTime(start, end, nextSample, samples)) >= 0) {
                    double target = sampleTime(start, end, nextSample, samples);
                    double fraction = (target - previousTime) / (time - previousTime);
                    double[] interpolated = new double[dimension];
                    for (int component = 0; component < dimension; component++) {
                        interpolated[component] = previous[component] + fraction * (state[component] - previous[component]);
                    }
                    sampledStates.add(interpolated);
                    sampledTimes.add(target);
                    nextSample++;
                }
                if (norm(state) > escapeRadius) {
                    escaped = true;
                    break;
                }
                if (steps % 256 == 0) {
                    progress.checkpoint(Math.abs(time - start) / span, "integrating");
                }
            }
            double factor = error == 0 ? 5 : Math.min(5, Math.max(0.2, 0.9 * Math.pow(error, -0.2)));
            step *= factor;
            if (Math.abs(step) < span * 1e-14) {
                escaped = true;
                break;
            }
        }
        double[] times = sampledTimes.stream().mapToDouble(Double::doubleValue).toArray();
        return new Solution(times, sampledStates.toArray(new double[0][]), escaped, steps);
    }

    private static double sampleTime(double start, double end, int index, int samples) {
        return start + (end - start) * index / (samples - 1);
    }

    private static double norm(double[] state) {
        double sum = 0;
        for (double value : state) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }
}
