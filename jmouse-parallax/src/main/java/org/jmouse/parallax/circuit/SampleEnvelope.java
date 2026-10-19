package org.jmouse.parallax.circuit;

/**
 * The lowest and highest voltage of every node since the sample before, over every step the solver took between — what
 * a digital oscilloscope's peak detection keeps. A sample is one instant; a 20 kHz square read once every 100 µs lands
 * on either level at random and draws a saw that is not there. {@code means} is each node's mean over the same
 * interval — an averaging oscilloscope's sample: the same square read once every 10 µs, five times a period, averaged
 * 2 V for 1.5. In the order the solver reports its nodes.
 *
 * @param differenceLows  the peaks of each difference of two nodes watched, in the order given, NaN where none could be
 *                        kept. A probe across a part reads the difference of two nodes, and its peaks are not the
 *                        difference of theirs: two nodes moving apart made the lowest of one less the highest of the
 *                        other a band twice as wide as the signal, thickest on its slopes.
 */
public record SampleEnvelope(double[] lows, double[] highs, double[] means, double[] differenceLows, double[] differenceHighs) {

    public SampleEnvelope(double[] lows, double[] highs, double[] means) {
        this(lows, highs, means, new double[0], new double[0]);
    }
}
