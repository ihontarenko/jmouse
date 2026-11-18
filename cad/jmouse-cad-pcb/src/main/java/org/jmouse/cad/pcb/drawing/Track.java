package org.jmouse.cad.pcb.drawing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.drawing.Arrangement;

import java.util.Collections;
import java.util.List;

/**
 * A track: straight copper run through points on one layer, its corners rounded.
 *
 * <p>Its corners are rounded by {@code rounding}, the radius asked of them; a corner that cannot keep its gaps so keeps a smaller one
 * of its own ({@code corners}), so one tight corner — a bus's inner line — does not hold every other corner of the track back.</p>
 *
 * @param layer       the copper layer it is on: {@code top}, {@code inner-1} …, {@code bottom}
 * @param width       how wide it is, millimetres
 * @param rounding    the radius its corners are asked to be rounded with, millimetres; 0 for sharp corners
 * @param points      the points it runs through, in order, at least two
 * @param arrangement the group it is chosen and moved with, and whether it is locked where it stands
 * @param finish      what its copper is left as: bare, the solder mask open along it for solder to be run on; empty for under the mask
 * @param corners     the radius each corner between its points keeps, in order, none above {@code rounding}; empty where every corner
 *                    keeps {@code rounding}
 */
public record Track(String layer, double width, double rounding, List<Point> points, Arrangement arrangement, String finish,
                    List<Double> corners) {

    public Track {
        points = List.copyOf(points);
        arrangement = arrangement == null ? Arrangement.FREE : arrangement;
        finish = finish == null ? "" : finish;
        corners = cornersOf(rounding, points.size(), corners == null ? List.of() : corners);
    }

    /** A track in no group, free, under the mask, every corner at its rounding. */
    public Track(String layer, double width, double rounding, List<Point> points) {
        this(layer, width, rounding, points, Arrangement.FREE, "", List.of());
    }

    /** A track every corner of which keeps its rounding. */
    public Track(String layer, double width, double rounding, List<Point> points, Arrangement arrangement, String finish) {
        this(layer, width, rounding, points, arrangement, finish, List.of());
    }

    /**
     * Corners as kept: none where every one keeps the rounding; each at most the rounding. Refused where they are not one a corner.
     */
    private static List<Double> cornersOf(double rounding, int points, List<Double> corners) {
        if (corners.isEmpty()) {
            return List.of();
        }
        if (corners.size() != Math.max(0, points - 2)) {
            throw new IllegalArgumentException("A track of " + points + " points has " + Math.max(0, points - 2) + " corners, not " + corners.size());
        }
        List<Double> kept = corners.stream().map(radius -> Math.max(0, Math.min(radius, rounding))).toList();
        return kept.stream().allMatch(radius -> radius == rounding) ? List.of() : kept;
    }

    /** The radius a corner keeps, by its place among the corners: the first corner is the track's second point. */
    public double roundingAt(int corner) {
        return corners.isEmpty() ? rounding : corners.get(corner);
    }

    /** The radius the corners keep at least: the rounding where every one keeps it. */
    public double leastRounding() {
        return corners.stream().mapToDouble(Double::doubleValue).min().orElse(rounding);
    }

    /** The same track standing otherwise among the others. */
    public Track arranged(Arrangement other) {
        return new Track(layer, width, rounding, points, other, finish, corners);
    }

    public boolean bare() {
        return PcbVocabulary.FINISH_BARE.equals(finish);
    }

    /**
     * The same track run through other points: its corners keep their own radii where there are as many of them; else every one keeps
     * the least any kept, so a track moved never rounds more than it fitted.
     */
    public Track withPoints(List<Point> others) {
        if (corners.isEmpty() || corners.size() == Math.max(0, others.size() - 2)) {
            return new Track(layer, width, rounding, others, arrangement, finish, corners);
        }
        double least = leastRounding();
        return new Track(layer, width, rounding, others, arrangement, finish, Collections.nCopies(Math.max(0, others.size() - 2), least));
    }

    /** The same track with every corner asked to be rounded so. */
    public Track withRounding(double other) {
        return new Track(layer, width, other, points, arrangement, finish, List.of());
    }

    /** The same track, its corners asked to be rounded so, each keeping the radius it fits. */
    public Track withCorners(double asked, List<Double> kept) {
        return new Track(layer, width, asked, points, arrangement, finish, kept);
    }

    /** The same track as wide as asked, its corners as they are. */
    public Track withWidth(double other) {
        return new Track(layer, other, rounding, points, arrangement, finish, corners);
    }
}
