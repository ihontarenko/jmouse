package org.jmouse.cad.plugin.odb;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.exchange.pcb.ArtworkFeature;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.odb.OdbComponents;
import org.jmouse.odb.OdbEda;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A pcb's connectivity as a job says it, gathered while its layers are written: each copper feature in its net's subnet — a
 * run of track a trace, a poured area a plane, a via's discs on every layer one via, a part's pad a toeprint —, each hole in
 * the subnet that stands at it; then every seated part as a component of a package.
 *
 * <p>⚠️ A footprint's frame is Y down and turns counter-clockwise; a job's is Y up and turns clockwise, a part on the bottom
 * mirrored in X first. {@code TOP}'s place is worked out by the placement itself, so it and {@code CMP} cannot disagree with
 * the pcb.</p>
 */
final class PcbOdbConnectivity {

    /** A subnet of a net. */
    private record Place(int net, int subnet) {
    }

    private final PcbFabrication         fabrication;
    private final OdbEda                   eda;
    private final List<String>             notes;
    /** Where each seated part is in its side's file: its number there. */
    private final Map<String, Integer>     componentNumbers = new HashMap<>();
    private final Map<String, Place>       toeprints        = new HashMap<>();
    private final Map<String, Place>       vias             = new HashMap<>();
    /** The subnet standing at a point, for the hole drilled there. */
    private final Map<String, Place>       standing         = new HashMap<>();

    PcbOdbConnectivity(PcbFabrication fabrication, OdbEda eda, List<String> notes) {
        this.fabrication = fabrication;
        this.eda = eda;
        this.notes = notes;
        Map<OdbComponents.Side, Integer> counts = new HashMap<>();
        for (PcbFabrication.SeatedPart part : fabrication.parts()) {
            if (part.drawing().isPresent()) {
                componentNumbers.put(part.designator(), counts.merge(sideOf(part.placement()), 1, Integer::sum) - 1);
            }
        }
    }

    /** The features numbered from {@code first}, up to but not {@code end}, that one drawn thing became on a copper layer. */
    void copper(ArtworkFeature feature, String layer, int first, int end) {
        Place place = switch (feature) {
            case ArtworkFeature.Pad pad -> toeprintOf(pad.placement(), pad.pad(), pad.net());
            case ArtworkFeature.Disc disc -> viaAt(disc);
            case ArtworkFeature.Region region -> {
                int net = eda.net(region.net());
                yield new Place(net, eda.subnet(net, new OdbEda.Plane()));
            }
            case ArtworkFeature.Stroke stroke -> trace(stroke.net());
            case ArtworkFeature.Arc arc -> trace(arc.net());
        };
        for (int number = first; number < end; number++) {
            eda.feature(place.net(), place.subnet(), OdbEda.FeatureType.COPPER, layer, number);
        }
    }

    /** A hole, in the subnet that stands at it; a hole nothing stands at is no part of any net. */
    void hole(Point at, String layer, int number) {
        Place place = standing.get(key(at.x(), at.y()));
        if (place != null) {
            eda.feature(place.net(), place.subnet(), OdbEda.FeatureType.HOLE, layer, number);
        }
    }

    /** Every seated part of a side, with its package; every pad a toeprint, on its net or on none. */
    Optional<OdbComponents> components(OdbComponents.Side side) {
        OdbComponents written = new OdbComponents(side);
        boolean any = false;
        for (PcbFabrication.SeatedPart part : fabrication.parts()) {
            Placement placement = part.placement();
            if (part.drawing().isEmpty()) {
                notes.add(part.designator() + " has no footprint to read, so it is no component of the job.");
                continue;
            }
            if (sideOf(placement) != side) {
                continue;
            }
            FootprintDrawing drawing = part.drawing().get();
            List<OdbComponents.Toeprint> toeprintsOfPart = new ArrayList<>();
            for (int index = 0; index < drawing.pads().size(); index++) {
                FootprintDrawing.Pad pad = drawing.pads().get(index);
                Place place = toeprintOf(placement, pad, part.padNets().get(pad.number()));
                Point at = placement.placed(pad.x(), pad.y());
                toeprintsOfPart.add(new OdbComponents.Toeprint(index, at.x(), upward(at.y()), place.net(), place.subnet(), pad.number()));
            }
            written.add(new OdbComponents.Component(eda.packageOf(packageOf(part, drawing)), placement.x(), upward(placement.y()),
                    clockwise(placement.rotation()), part.printed(), part.component(), toeprintsOfPart));
            any = true;
        }
        return any ? Optional.of(written) : Optional.empty();
    }

    private Place trace(String net) {
        int number = eda.net(net);
        return new Place(number, eda.subnet(number, new OdbEda.Trace()));
    }

    /** A disc at one place is one via, however many layers it is on. */
    private Place viaAt(ArtworkFeature.Disc disc) {
        String key = key(disc.centre().x(), disc.centre().y());
        return vias.computeIfAbsent(key + "@" + disc.net(), ignored -> {
            int net = eda.net(disc.net());
            Place place = new Place(net, eda.subnet(net, new OdbEda.Via()));
            standing.put(key, place);
            return place;
        });
    }

    /** A pad of a part is one toeprint, however many layers it is on. */
    private Place toeprintOf(Placement placement, FootprintDrawing.Pad pad, String net) {
        int toeprint = fabrication.parts().stream().filter(part -> part.designator().equals(placement.designator())).findFirst()
                .flatMap(PcbFabrication.SeatedPart::drawing).map(drawing -> drawing.pads().indexOf(pad)).orElse(-1);
        return toeprints.computeIfAbsent(placement.designator() + "/" + toeprint, ignored -> {
            int number = eda.net(net);
            Place place = new Place(number, eda.subnet(number, new OdbEda.Toeprint(sideOf(placement), componentNumbers.get(placement.designator()), toeprint)));
            Point at = placement.placed(pad.x(), pad.y());
            standing.put(key(at.x(), at.y()), place);
            return place;
        });
    }

    private static OdbEda.Package packageOf(PcbFabrication.SeatedPart part, FootprintDrawing drawing) {
        List<OdbEda.Pin> pins = drawing.pads().stream().map(PcbOdbConnectivity::pinOf).toList();
        double minimumX = 0;
        double minimumY = 0;
        double maximumX = 0;
        double maximumY = 0;
        for (FootprintDrawing.Pad pad : drawing.pads()) {
            double reach = Math.hypot(pad.width(), pad.height()) / 2;
            minimumX = Math.min(minimumX, pad.x() - reach);
            maximumX = Math.max(maximumX, pad.x() + reach);
            minimumY = Math.min(minimumY, upward(pad.y()) - reach);
            maximumY = Math.max(maximumY, upward(pad.y()) + reach);
        }
        return new OdbEda.Package(part.footprint().orElse(part.component()), pitchOf(drawing.pads()),
                new OdbEda.Rectangle(minimumX, minimumY, maximumX - minimumX, maximumY - minimumY), pins);
    }

    private static OdbEda.Pin pinOf(FootprintDrawing.Pad pad) {
        OdbEda.PinType type = pad.isSurface() ? OdbEda.PinType.SURFACE : OdbEda.PinType.THROUGH_HOLE;
        OdbEda.PinElectrical electrical = pad.isSurface() || pad.isPlatedThrough() ? OdbEda.PinElectrical.ELECTRICAL : OdbEda.PinElectrical.MECHANICAL;
        OdbEda.PinMount mount = pad.isSurface() ? OdbEda.PinMount.SURFACE
                : pad.isPlatedThrough() ? OdbEda.PinMount.THROUGH_HOLE : OdbEda.PinMount.HOLE;
        return new OdbEda.Pin(pad.number(), type, pad.x(), upward(pad.y()), electrical, mount, outlineOf(pad));
    }

    /** A pad as a rectangle while it stands square to its package, else the circle round it. */
    private static OdbEda.Outline outlineOf(FootprintDrawing.Pad pad) {
        double turn = ((pad.rotation() % 180) + 180) % 180;
        if (turn != 0 && turn != 90) {
            return new OdbEda.Circle(pad.x(), upward(pad.y()), Math.hypot(pad.width(), pad.height()) / 2);
        }
        double width = turn == 0 ? pad.width() : pad.height();
        double height = turn == 0 ? pad.height() : pad.width();
        return new OdbEda.Rectangle(pad.x() - width / 2, upward(pad.y()) - height / 2, width, height);
    }

    private static double pitchOf(List<FootprintDrawing.Pad> pads) {
        double nearest = Double.MAX_VALUE;
        for (int first = 0; first < pads.size(); first++) {
            for (int second = first + 1; second < pads.size(); second++) {
                nearest = Math.min(nearest, Math.hypot(pads.get(first).x() - pads.get(second).x(), pads.get(first).y() - pads.get(second).y()));
            }
        }
        return nearest == Double.MAX_VALUE ? 0 : nearest;
    }

    private static OdbComponents.Side sideOf(Placement placement) {
        return placement.onBottom() ? OdbComponents.Side.BOTTOM : OdbComponents.Side.TOP;
    }

    /** Turned counter-clockwise on the pcb, as a job says it: clockwise. */
    private static double clockwise(double counterClockwise) {
        double turned = ((-counterClockwise % 360) + 360) % 360;
        return turned == 360 ? 0 : turned;
    }

    private static double upward(double y) {
        return y == 0 ? 0 : -y;
    }

    private static String key(double x, double y) {
        return String.format(Locale.ROOT, "%.4f,%.4f", x, y);
    }
}
