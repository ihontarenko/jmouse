package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Getting out of a fine pitch: where a track of its net's width cannot stand on a pad and keep its gap from the pads beside it,
 * it leaves by a neck — thinner, as thin as the rules allow if it must —, straight, to the first point where the full width
 * fits. As a person fans a package out. The rest of the way is laid from there.
 */
public final class FanOut {

    private FanOut() {
    }

    /** A neck out of an end: how wide, and its two ends. */
    public record Neck(double width, Point from, Point to) {
    }

    /** Where a way of a width starts from an end: the end itself, or the far end of a neck. */
    public record FannedEnd(Point at, Optional<Neck> neck) {
    }

    /** Where a way of a width starts from an end; empty where no neck gets out. */
    public static Optional<FannedEnd> fannedOut(RoutingWorld world, String layer, Optional<String> net, double width, Point end) {
        TrackFence full = world.fence(layer, net, width);
        if (full.pointClear(end)) {
            return Optional.of(new FannedEnd(end, Optional.empty()));
        }
        RoutingSettings.Routing settings = world.settings().routing();
        double least = world.rules().value(PcbVocabulary.RULE_LEAST_TRACK_WIDTH);
        List<Double> widths = new ArrayList<>();
        for (double share : settings.neckShares()) {
            double candidate = Math.max(least, width * share);
            if (candidate < width) {
                widths.add(candidate);
            }
        }
        for (double neckWidth : widths) {
            TrackFence neck = world.fence(layer, net, neckWidth);
            if (!neck.pointClear(end)) {
                continue;
            }
            Point best = null;
            double shortest = Double.POSITIVE_INFINITY;
            for (Point way : Lines.STRAIGHT_FIRST_WAYS) {
                for (double reach = settings.fanOutStep(); reach <= settings.fanOutReach() && reach < shortest; reach += settings.fanOutStep()) {
                    Point point = Lines.offset(end, way, reach);
                    if (!neck.lineClear(List.of(end, point))) {
                        break;
                    }
                    if (full.pointClear(point)) {
                        best = Lines.settled(point);
                        shortest = reach;
                        break;
                    }
                }
            }
            if (best != null) {
                return Optional.of(new FannedEnd(best, Optional.of(new Neck(neckWidth, end, best))));
            }
        }
        return Optional.empty();
    }
}
