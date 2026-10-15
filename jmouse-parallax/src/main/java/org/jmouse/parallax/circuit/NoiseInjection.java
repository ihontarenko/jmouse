package org.jmouse.parallax.circuit;

import java.util.List;
import java.util.SplittableRandom;

/**
 * The noise of a realistic bench in a transient: every other device's noise currents ({@link Device#noise}), each a
 * random current of the power its density gives over the band the samples resolve — half the sample rate. A value is
 * drawn at each sample instant and joined to the next by a straight line, so the current is continuous and a step never
 * meets an edge; each value comes from the seed, the source and the instant alone, so a step tried again, or a run
 * continued, meets the same noise. The densities are read again from the state at every sample instant, so a junction's
 * shot noise follows its current. The 1/f part of a density is left out here: the band a transient resolves starts far
 * above any corner it would matter at.
 */
final class NoiseInjection implements Device {

    private final List<Device> noisy;
    private final double       interval;
    private final double       temperature;
    private final long         seed;
    private long               readAt = Long.MIN_VALUE;
    private List<NoiseCurrent> currents = List.of();
    /** Each source's standard normal value at the instant read and at the next one, drawn once per instant. */
    private double[]           drawnAtInstant = new double[0];
    private double[]           drawnAtNext = new double[0];

    /**
     * @param noisy       the devices whose noise is injected
     * @param interval    the time between two samples, seconds: the band is half its inverse
     * @param temperature the bench's temperature, kelvin
     */
    NoiseInjection(List<Device> noisy, double interval, double temperature, long seed) {
        this.noisy = List.copyOf(noisy);
        this.interval = interval;
        this.temperature = temperature;
        this.seed = seed;
    }

    @Override
    public void stamp(Assembly assembly) {
        if (!assembly.isTransient()) {
            return;
        }
        double position = assembly.time() / interval;
        long instant = (long) Math.floor(position);
        double share = position - instant;
        if (instant != readAt) {
            currents = read(assembly.previousState());
            readAt = instant;
            drawAt(instant);
        }
        double band = 1 / (2 * interval);
        for (int source = 0; source < currents.size(); source++) {
            NoiseCurrent current = currents.get(source);
            double amplitude = Math.sqrt(current.density() * band);
            double drawn = (1 - share) * drawnAtInstant[source] + share * drawnAtNext[source];
            assembly.current(current.positive(), current.negative(), amplitude * drawn);
        }
    }

    @Override
    public void stampPhasor(PhasorAssembly assembly) {
    }

    /** The current must be followed between its sample instants, so a step is never longer than one. */
    @Override
    public double maximumStep() {
        return interval;
    }

    private List<NoiseCurrent> read(double[] state) {
        return noisy.stream().flatMap(device -> device.noise(state, temperature).stream()).toList();
    }

    /** Every source's values at this instant and the next, drawn once: a step is stamped many times between two instants. */
    private void drawAt(long instant) {
        if (drawnAtInstant.length != currents.size()) {
            drawnAtInstant = new double[currents.size()];
            drawnAtNext = new double[currents.size()];
        }
        for (int source = 0; source < currents.size(); source++) {
            drawnAtInstant[source] = drawn(source, instant);
            drawnAtNext[source] = drawn(source, instant + 1);
        }
    }

    /** A standard normal value for one source at one sample instant, the same every time it is asked for. */
    private double drawn(int source, long instant) {
        long mixed = seed * 0x9E3779B97F4A7C15L + source * 0xC2B2AE3D27D4EB4FL + instant;
        return new SplittableRandom(mixed).nextGaussian();
    }
}
