package org.jmouse.cad.pcb.routing;

import java.util.ArrayList;
import java.util.List;

/**
 * 〰️ The shape a track is bent into to lengthen it, bump by bump, as a layout program offers it: what one bump is, along the segment and
 * across it, and to which side each bump goes. {@link LengthTuning} lays the bumps and finds how tall they must be; the pattern says
 * only what a bump is, so a new shape is a new constant here and nothing else.
 *
 * <p>A bump stands on the segment from {@code left} to {@code left + spacing} along it and reaches {@code height} across it; the
 * spacing after it, back on the segment, keeps the next one clear. Every point a bump gives is {@code {along, across}}.</p>
 */
public enum TuningPattern {

    /** Square bumps to one side: up, over, down — the trombone KiCad lays. */
    SQUARE(0.5) {
        @Override
        List<double[]> bump(double left, double spacing, double height) {
            return List.of(new double[] {left, 0}, new double[] {left, height}, new double[] {left + spacing, height}, new double[] {left + spacing, 0});
        }
    },

    /** Square bumps to either side in turn — an accordion: half as tall for the same length, to both sides of the segment. */
    ACCORDION(0.5) {
        @Override
        List<double[]> bump(double left, double spacing, double height) {
            return SQUARE.bump(left, spacing, height);
        }

        @Override
        int sideOf(int bump, int side) {
            return bump % 2 == 0 ? side : -side;
        }
    },

    /** Bumps with their corners cut at 45°: no right angle for a fast edge to reflect from, as high-speed layouts want. */
    CHAMFERED(0.25) {
        @Override
        List<double[]> bump(double left, double spacing, double height) {
            double cut = Math.min(spacing / 2, height) / 2;
            return List.of(new double[] {left, 0}, new double[] {left, height - cut}, new double[] {left + cut, height},
                    new double[] {left + spacing - cut, height}, new double[] {left + spacing, height - cut}, new double[] {left + spacing, 0});
        }
    },

    /** Bumps whose tops are half circles across their width: no corner at all at the turn. */
    ROUNDED(0) {
        /** How many straight pieces a half circle is laid as: fine enough that its length is the arc's within the tolerance. */
        private static final int PIECES = 8;

        @Override
        List<double[]> bump(double left, double spacing, double height) {
            double radius = spacing / 2;
            double reach = Math.min(radius, height);
            double shoulder = height - reach;
            List<double[]> points = new ArrayList<>();
            points.add(new double[] {left, 0});
            for (int piece = 0; piece <= PIECES; piece++) {
                double angle = Math.PI * (1 - (double) piece / PIECES);
                points.add(new double[] {left + radius + radius * Math.cos(angle), shoulder + reach * Math.sin(angle)});
            }
            points.add(new double[] {left + spacing, 0});
            return points;
        }
    },

    /** Teeth up to a point and down again — a zigzag: the shortest bump of all for its height, for a little length in little room. */
    TRIANGLE(0) {
        @Override
        List<double[]> bump(double left, double spacing, double height) {
            return List.of(new double[] {left, 0}, new double[] {left + spacing / 2, height}, new double[] {left + spacing, 0});
        }
    };

    /**
     * How much of a bump's width a corner of the track may be rounded with before the bump loses its shape: half its width rounds a
     * square bump's top into a half circle; a zigzag's teeth and a rounded top take no rounding of their own at all.
     */
    private final double roundingShare;

    TuningPattern(double roundingShare) {
        this.roundingShare = roundingShare;
    }

    /** The track's rounding as far as bumps a spacing wide bear it: what the meander is laid and measured with. */
    double roundingFor(double rounding, double spacing) {
        return Precision.settled(Math.min(rounding, spacing * roundingShare));
    }

    /** The points of one bump, from where it leaves the segment to where it comes back to it. */
    abstract List<double[]> bump(double left, double spacing, double height);

    /** The side a bump goes to, by its number, of the side the meander is laid on: +1 the segment's left, -1 its right. */
    int sideOf(int bump, int side) {
        return side;
    }
}
