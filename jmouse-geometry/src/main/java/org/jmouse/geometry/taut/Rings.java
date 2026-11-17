package org.jmouse.geometry.taut;

import org.locationtech.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.List;

/** A closed ring walked along: a place on it is its side's number and how far along that side, as a share. */
final class Rings {

    private Rings() {
    }

    /** The way along a ring, in the order of its points, from one place on it to another — once round at most. */
    static List<Coordinate> arc(Coordinate[] ring, double from, double to) {
        int sides = ring.length - 1;
        double end = to < from ? to + sides : to;
        List<Coordinate> arc = new ArrayList<>();
        arc.add(pointAt(ring, from));
        for (int corner = (int) Math.floor(from) + 1; corner < end; corner++) {
            arc.add(ring[corner % sides]);
        }
        arc.add(pointAt(ring, end % sides));
        return Paths.withoutRepeats(arc);
    }

    private static Coordinate pointAt(Coordinate[] ring, double round) {
        int sides = ring.length - 1;
        int side = Math.min((int) Math.floor(round), sides - 1);
        double share = round - side;
        Coordinate from = ring[side];
        Coordinate to = ring[side + 1];
        return new Coordinate(from.x + (to.x - from.x) * share, from.y + (to.y - from.y) * share);
    }
}
