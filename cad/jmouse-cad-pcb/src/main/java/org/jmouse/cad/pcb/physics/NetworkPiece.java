package org.jmouse.cad.pcb.physics;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.geometry.Segments;

import java.util.ArrayList;
import java.util.List;

/** A straight piece of copper along one layer: a track's or a drawn line's segment, or an arc's end other copper meets it at. */
final class NetworkPiece {

    final CopperPaths.Thing thing;
    final int               index;
    final String            layer;
    final Point             from;
    final Point             to;
    final double            width;
    final CopperShape       shape;
    final int[]             ends;
    /** Whether it is a resistor of its own: a track's segment is; an arc's end only marks where the arc is met. */
    final boolean           conducts;
    /** Where along it other copper meets it: the share of its length, and the node there. */
    final List<double[]>    taps = new ArrayList<>();

    NetworkPiece(CopperPaths.Thing thing, int index, String layer, Point from, Point to, double width, CopperShape shape, int[] ends,
                 boolean conducts) {
        this.thing = thing;
        this.index = index;
        this.layer = layer;
        this.from = from;
        this.to = to;
        this.width = width;
        this.shape = shape;
        this.ends = ends;
        this.conducts = conducts;
    }

    /** The share along it of its point nearest a point. */
    double shareNearest(Point point) {
        return Segments.shareNearest(from.x(), from.y(), to.x(), to.y(), point.x(), point.y());
    }

    /** The shares along it and along another piece of their nearest points. */
    Segments.Shares nearestShares(NetworkPiece other) {
        return Segments.nearestShares(from.x(), from.y(), to.x(), to.y(), other.from.x(), other.from.y(), other.to.x(), other.to.y());
    }

    /** Its point a share of its length along. */
    Point at(double share) {
        return from.toward(to, share);
    }

    double length() {
        return from.distanceTo(to);
    }
}
