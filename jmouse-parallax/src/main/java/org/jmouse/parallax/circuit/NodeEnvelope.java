package org.jmouse.parallax.circuit;

import java.util.Arrays;
import java.util.List;

/**
 * What a digital oscilloscope's peak detection and averaging keep, for every node of a piece and every difference of
 * two of its nodes a probe watches: the lowest and highest voltage since the last read sample, over every accepted
 * step, and each node's voltage integrated through the run — the mean over a sample's interval is the difference of the
 * integral at its two ends, exact wherever a step crosses the sample.
 *
 * <p>A step brings its START into the peaks: its end may lie past the sample it reached, and belongs to the next
 * interval — taken as the end, an interval's band held a value up to a step ahead of it, and a sine grew teeth leading
 * the wave. The sample's own value closes the interval; the next step brings this end in.</p>
 */
final class NodeEnvelope {

    private final double[] nodeLows;
    private final double[] nodeHighs;
    private final double[] nodeIntegrals;
    private final double[] markedIntegrals;
    private double         markedTime = Double.NaN;
    /** Pairs of nodes, positive and negative (−1 for ground), whose difference's peaks are kept the same way. */
    private List<NodeDifference> differences = List.of();
    private double[] differenceLows = new double[0];
    private double[] differenceHighs = new double[0];

    NodeEnvelope(int nodes) {
        this.nodeLows = new double[nodes];
        this.nodeHighs = new double[nodes];
        this.nodeIntegrals = new double[nodes];
        this.markedIntegrals = new double[nodes];
        forget();
    }

    /** Keeps the peaks of these differences from the next step on. */
    void watch(List<NodeDifference> pairs) {
        differences = List.copyOf(pairs);
        differenceLows = new double[pairs.size()];
        differenceHighs = new double[pairs.size()];
        Arrays.fill(differenceLows, Double.POSITIVE_INFINITY);
        Arrays.fill(differenceHighs, Double.NEGATIVE_INFINITY);
    }

    /**
     * One accepted step: its start into every node's and every difference's peaks, and its stretch into the integrals —
     * held at its start where it landed on an edge, along the line between its ends otherwise.
     */
    void take(double[] start, double[] end, double duration, boolean landedOnEdge) {
        for (int node = 0; node < nodeLows.length; node++) {
            nodeLows[node] = Math.min(nodeLows[node], start[node]);
            nodeIntegrals[node] += (landedOnEdge ? start[node] : (start[node] + end[node]) / 2) * duration;
            nodeHighs[node] = Math.max(nodeHighs[node], start[node]);
        }
        for (int pair = 0; pair < differences.size(); pair++) {
            double difference = across(start, differences.get(pair));
            differenceLows[pair] = Math.min(differenceLows[pair], difference);
            differenceHighs[pair] = Math.max(differenceHighs[pair], difference);
        }
    }

    /**
     * The peaks since the last read sample, this sample's own voltages among them, and the next ones begun from them: an
     * interval holds both its ends. Begun empty, the interval a square wave falls in saw only the low level after the
     * fall — the step that lands on an edge ends at the new level — and its high half vanished from the peaks.
     *
     * @param voltages   the sample's own node voltages, at {@code at}
     * @param start      the last accepted step's start, and {@code end} its end, at {@code endTime}
     * @param landedOnEdge whether that step landed on an edge
     */
    SampleEnvelope read(double[] voltages, double at, double[] start, double[] end, double endTime, boolean landedOnEdge) {
        double[] lows = new double[voltages.length];
        double[] highs = new double[voltages.length];
        double[] means = new double[voltages.length];
        for (int node = 0; node < voltages.length; node++) {
            lows[node] = Math.min(nodeLows[node], voltages[node]);
            highs[node] = Math.max(nodeHighs[node], voltages[node]);
            // The integral at the sample, taken back from the step's end along the line to the sample's value.
            double integral = nodeIntegrals[node] - (landedOnEdge ? start[node] : (end[node] + voltages[node]) / 2) * (endTime - at);
            means[node] = Double.isNaN(markedTime) || !(at > markedTime) ? voltages[node] : (integral - markedIntegrals[node]) / (at - markedTime);
            markedIntegrals[node] = integral;
        }
        markedTime = at;
        System.arraycopy(voltages, 0, nodeLows, 0, voltages.length);
        System.arraycopy(voltages, 0, nodeHighs, 0, voltages.length);
        double[] lowsAcross = new double[differences.size()];
        double[] highsAcross = new double[differences.size()];
        for (int pair = 0; pair < differences.size(); pair++) {
            double difference = across(voltages, differences.get(pair));
            lowsAcross[pair] = Math.min(differenceLows[pair], difference);
            highsAcross[pair] = Math.max(differenceHighs[pair], difference);
            differenceLows[pair] = difference;
            differenceHighs[pair] = difference;
        }
        return new SampleEnvelope(lows, highs, means, lowsAcross, highsAcross);
    }

    /** Forgets every peak and the mark: what unread samples went through is no read sample's peak. */
    void forget() {
        markedTime = Double.NaN;
        Arrays.fill(nodeLows, Double.POSITIVE_INFINITY);
        Arrays.fill(nodeHighs, Double.NEGATIVE_INFINITY);
        Arrays.fill(differenceLows, Double.POSITIVE_INFINITY);
        Arrays.fill(differenceHighs, Double.NEGATIVE_INFINITY);
    }

    /** One watched difference in these voltages. */
    private static double across(double[] voltages, NodeDifference pair) {
        return voltageOf(voltages, pair.positive()) - voltageOf(voltages, pair.negative());
    }

    /** A node's voltage among these; ground, −1, is 0. */
    private static double voltageOf(double[] voltages, int node) {
        return node < 0 ? 0 : voltages[node];
    }
}
