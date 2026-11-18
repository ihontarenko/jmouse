package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.Point;
import org.jmouse.cad.exchange.pcb.ArtworkFeature;
import org.jmouse.gerber.GerberPoint;
import org.jmouse.gerber.GerberShape;
import org.jmouse.gerber.compose.GerberComposer;

import java.util.List;
import java.util.Optional;

/**
 * The pcb's artwork as Gerber draws it: a stroke and an arc with a round aperture, a pad by {@link PadShapes}, a disc
 * flashed, a region filled dark with its holes cleared — each aperture named for what its feature is for.
 */
final class ArtworkGerber {

    private final PadShapes pads;

    ArtworkGerber(PadShapes pads) {
        this.pads = pads;
    }

    /** A layer's file: every feature in the order given, the later drawn over the earlier. */
    String write(String function, boolean positive, List<ArtworkFeature> features) {
        GerberComposer composer = PcbGerber.file(function, positive);
        for (ArtworkFeature feature : features) {
            draw(composer, feature);
        }
        return composer.document().toSource();
    }

    private void draw(GerberComposer composer, ArtworkFeature feature) {
        String purpose = purposeOf(feature.role());
        composer.net(feature.net());
        switch (feature) {
            case ArtworkFeature.Stroke stroke -> composer.line(composer.aperture(new GerberShape.Circle(stroke.width()), purpose),
                    stroke.points().stream().map(ArtworkGerber::out).toList());
            case ArtworkFeature.Arc arc -> arc(composer, arc, purpose);
            case ArtworkFeature.Pad pad -> pads.draw(composer, pad.placement(), pad.pad(), pad.growth(), purpose);
            case ArtworkFeature.Disc disc -> composer.flash(composer.aperture(new GerberShape.Circle(disc.diameter()), purpose),
                    out(disc.centre()));
            case ArtworkFeature.Region region -> region(composer, region);
        }
    }

    /** An arc through its three points; three in a line are a straight run. */
    private static void arc(GerberComposer composer, ArtworkFeature.Arc arc, String purpose) {
        int pen = composer.aperture(new GerberShape.Circle(arc.width()), purpose);
        GerberPoint from = out(arc.from());
        GerberPoint to = out(arc.to());
        Optional<ArtworkFeature.Arc.Circle> circle = arc.circle();
        if (circle.isEmpty()) {
            composer.line(pen, List.of(from, to));
            return;
        }
        composer.arc(pen, from, to, out(circle.get().centre()), circle.get().clockwise());
    }

    /** A region's outline dark, its holes cleared: whatever a later region draws inside a hole stays. */
    private static void region(GerberComposer composer, ArtworkFeature.Region region) {
        composer.region(List.of(region.outline().stream().map(ArtworkGerber::out).toList()));
        if (region.holes().isEmpty()) {
            return;
        }
        composer.polarity(false);
        for (List<Point> hole : region.holes()) {
            composer.region(List.of(hole.stream().map(ArtworkGerber::out).toList()));
        }
        composer.polarity(true);
    }

    /** The aperture function Gerber X2 records for a feature's role; none for an opening in a mask or a stencil. */
    private static String purposeOf(ArtworkFeature.Role role) {
        return switch (role) {
            case CONDUCTOR -> "Conductor";
            case SURFACE_PAD -> "SMDPad,CuDef";
            case COMPONENT_PAD -> "ComponentPad";
            case VIA_PAD -> "ViaPad";
            case LEGEND -> "Other,Legend";
            case OPENING -> null;
        };
    }

    private static GerberPoint out(Point point) {
        return PcbGerber.out(point.x(), point.y());
    }
}
