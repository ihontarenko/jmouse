package org.jmouse.cad.pcb.drawing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.drawing.Arrangement;
import java.util.List;

/**
 * A track: straight copper run through points on one layer.
 *
 * @param layer       the copper layer it is on: {@code top}, {@code inner-1} …, {@code bottom}
 * @param width       how wide it is, millimetres
 * @param rounding    how far its corners are rounded, millimetres; 0 for sharp corners
 * @param points      the points it runs through, in order, at least two
 * @param arrangement the group it is chosen and moved with, and whether it is locked where it stands
 * @param finish      what its copper is left as: bare, the solder mask open along it for solder to be run on; empty for under the mask
 */
public record Track(String layer, double width, double rounding, List<Point> points, Arrangement arrangement, String finish) {

    public Track {
        points = List.copyOf(points);
        arrangement = arrangement == null ? Arrangement.FREE : arrangement;
        finish = finish == null ? "" : finish;
    }

    /** A track in no group, free, under the mask. */
    public Track(String layer, double width, double rounding, List<Point> points) {
        this(layer, width, rounding, points, Arrangement.FREE, "");
    }

    /** The same track standing otherwise among the others. */
    public Track arranged(Arrangement other) {
        return new Track(layer, width, rounding, points, other, finish);
    }

    public boolean bare() {
        return PcbVocabulary.FINISH_BARE.equals(finish);
    }

    /** The same track run through other points. */
    public Track withPoints(List<Point> others) {
        return new Track(layer, width, rounding, others, arrangement, finish);
    }

    /** The same track with its corners rounded so. */
    public Track withRounding(double other) {
        return new Track(layer, width, other, points, arrangement, finish);
    }
}
