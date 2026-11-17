package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
import org.jmouse.geometry.taut.ObstacleField;
import org.jmouse.geometry.taut.TautPath;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import java.util.ArrayList;
import java.util.List;

/**
 * A drag's bands pulled along with it: the moved parts taken from where they stood to where they are let go a step at a time —
 * each step shorter than what a band keeps clear by, so no copper passes through a band between two pulls —, every band pulled
 * tight after each step among the others as they then run, the moving ones first, which push the still ones ahead of them.
 *
 * <p>A band pushed onto another — a pad driving it against the next line — pushes that one on in turn: where it has no room among
 * the bands it is laid among the solid copper alone, and the bands it then comes into are pulled again round it, pass after pass,
 * until none is in another. A band that has no room even so is squeezed: it stays as it was, its ends going with their pads, and
 * pulls no more.</p>
 */
final class BandPulling {

    private final TautPath                pulling;
    private final RoutingSettings.Banding settings;

    BandPulling(RoutingSettings.Banding settings) {
        this.settings = settings;
        this.pulling = new TautPath(settings.margin());
    }

    List<Band> pulled(List<Band> bands, BandField field, DragMotion motion, double farthest) {
        List<Band> pulled = new ArrayList<>(bands);
        int steps = stepsFor(farthest, field.leastReach(bands));
        for (int step = 1; step <= steps; step++) {
            double share = (double) step / steps;
            // The first pass pulls every band a step along; another pulls again only the bands near one that was pushed into others,
            // and of those only the ones it came into.
            List<Band> pushers = List.of();
            for (int pass = 0; pass < settings.pushPasses() && (pass == 0 || !pushers.isEmpty()); pass++) {
                List<Band> pushedNow = new ArrayList<>();
                for (int index : movingFirst(pulled)) {
                    Band band = pulled.get(index);
                    List<Point> path = band.pathAt(motion, share);
                    if (pass > 0 && (!nearAny(band, pushers, field) || !cameInto(path, field.of(band, path, pulled, share, BandField.OtherBands.KEPT_CLEAR)))) {
                        continue;
                    }
                    Pulled once = pulledOnce(band, path, pulled, field, share);
                    pulled.set(index, once.band());
                    if (once.pushed()) {
                        pushedNow.add(once.band());
                    }
                }
                pushers = pushedNow;
            }
        }
        return pulled;
    }

    /** A band as one pull left it, and whether it was laid pushing the other bands. */
    private record Pulled(Band band, boolean pushed) {
    }

    /** One band pulled tight a share of the way along: among the other bands; where it has no room there, pushing them. */
    private Pulled pulledOnce(Band band, List<Point> path, List<Band> bands, BandField field, double share) {
        if (band.tautness() == TautPath.Tautness.SQUEEZED) {
            return new Pulled(band.running(path, TautPath.Tautness.SQUEEZED), false);
        }
        TautPath.Pull among = pulling.pulled(Plane.coordinatesOf(path), field.of(band, path, bands, share, BandField.OtherBands.KEPT_CLEAR));
        if (among.tautness() != TautPath.Tautness.SQUEEZED) {
            return new Pulled(band.running(Plane.pointsOf(among.path()), among.tautness()), false);
        }
        TautPath.Pull pushing = pulling.pulled(Plane.coordinatesOf(path), field.of(band, path, bands, share, BandField.OtherBands.PUSHED));
        return pushing.tautness() == TautPath.Tautness.SQUEEZED
                ? new Pulled(band.running(path, TautPath.Tautness.SQUEEZED), false)
                : new Pulled(band.running(Plane.pointsOf(pushing.path()), pushing.tautness()), true);
    }

    /** Whether a band runs near any of these — within what the two keep clear by — on its layer, of another net. */
    private static boolean nearAny(Band band, List<Band> others, BandField field) {
        LineString line = lineOf(band.fullPath());
        for (Band other : others) {
            boolean ofAnother = band.net().isEmpty() || !other.net().equals(band.net());
            if (other.track() != band.track() && ofAnother && other.layer().equals(band.layer())
                && line.isWithinDistance(lineOf(other.fullPath()), field.keptApart(band, other))) {
                return true;
            }
        }
        return false;
    }

    private static LineString lineOf(List<Point> path) {
        return Plane.FACTORY.createLineString(Plane.coordinatesOf(path).toArray(Coordinate[]::new));
    }

    /** Whether another band has come into a band's path: something of what it keeps clear of now stands across it. */
    private static boolean cameInto(List<Point> path, ObstacleField field) {
        return field.area().relate(lineOf(path)).matches("T********");
    }

    /** The bands' places, the moving ones first: a still one is pulled among the moving ones as they then run. */
    private static List<Integer> movingFirst(List<Band> bands) {
        List<Integer> order = new ArrayList<>();
        for (int index = 0; index < bands.size(); index++) {
            if (bands.get(index).moving()) {
                order.add(index);
            }
        }
        for (int index = 0; index < bands.size(); index++) {
            if (!bands.get(index).moving()) {
                order.add(index);
            }
        }
        return order;
    }

    /** As many steps as keep each shorter than a share of the least a band keeps clear by — once at least, never past the most. */
    private int stepsFor(double farthest, double leastReach) {
        double step = settings.stepShare() * leastReach;
        int needed = step > 0 ? (int) Math.ceil(farthest / step) : 1;
        return Math.max(1, Math.min(settings.mostSteps(), needed));
    }
}



