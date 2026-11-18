package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.locationtech.jts.geom.Envelope;

import java.util.List;

/**
 * A line cut at its corners round the region a drag works in, as KiCad keeps its optimizer to the area a drag changed: every
 * stretch that comes into the region is the drag's to pull, and the last corner before the first of them and the first corner after
 * the last of them hold it; what runs beyond those corners stands as it ran. Cut at corners the line already had, it never gets one
 * it did not have where it is cut.
 */
final class RegionCut {

    private RegionCut() {
    }

    /** How much of a line's corners a region reaches; the whole line where no stretch of it comes into the region. */
    static BandReach of(List<Point> line, Envelope region, BandEnd lineStart, BandEnd lineEnd) {
        int first = -1;
        int last = -1;
        for (int stretch = 0; stretch + 1 < line.size(); stretch++) {
            if (comesInto(line.get(stretch), line.get(stretch + 1), region)) {
                first = first < 0 ? stretch : first;
                last = stretch;
            }
        }
        if (first < 0) {
            return BandReach.whole(line, lineStart, lineEnd);
        }
        return new BandReach(line, first, last + 1, lineStart, lineEnd);
    }

    /** Whether a stretch comes into a region: its box meets the region and the stretch itself passes through it. */
    private static boolean comesInto(Point from, Point to, Envelope region) {
        if (!region.intersects(new Envelope(from.x(), to.x(), from.y(), to.y()))) {
            return false;
        }
        if (region.contains(from.x(), from.y()) || region.contains(to.x(), to.y())) {
            return true;
        }
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        double enter = 0;
        double leave = 1;
        double[] across = {-dx, dx, -dy, dy};
        double[] room = {from.x() - region.getMinX(), region.getMaxX() - from.x(), from.y() - region.getMinY(), region.getMaxY() - from.y()};
        for (int side = 0; side < 4; side++) {
            if (across[side] == 0) {
                if (room[side] < 0) {
                    return false;
                }
                continue;
            }
            double share = room[side] / across[side];
            if (across[side] < 0) {
                enter = Math.max(enter, share);
            } else {
                leave = Math.min(leave, share);
            }
        }
        return enter <= leave;
    }
}
