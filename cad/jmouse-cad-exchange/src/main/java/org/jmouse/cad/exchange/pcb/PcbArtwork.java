package org.jmouse.cad.exchange.pcb;

import org.jmouse.cad.settings.SettingsOwner;
import org.jmouse.cad.settings.SettingsSection;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.PadOutline;
import org.jmouse.cad.pcb.geometry.RoundedTrack;
import org.jmouse.cad.pcb.checks.PcbChecks;
import org.jmouse.cad.pcb.checks.PcbFinding;
import org.jmouse.cad.pcb.geometry.PlacedDesignator;
import org.jmouse.cad.pcb.geometry.Teardrops;
import org.jmouse.cad.pcb.geometry.ZonePours;
import org.jmouse.cad.exchange.pcb.ArtworkFeature.Role;
import org.jmouse.strokefont.StrokeFont;
import org.jmouse.strokefont.StrokeText;
import org.jmouse.svg.path.node.PathPolylines;
import org.jmouse.svg.path.parser.PathParser;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * What is drawn on each layer of a pcb as it is made — its copper, the openings of its solder mask, its paste and its
 * silkscreen —, as {@link ArtworkFeature}s every format draws its own way: Gerber as apertures and regions, a solid model
 * as areas. The one place that says what lies on a layer, so that the formats cannot disagree about it.
 */
public final class PcbArtwork {

    /** Its section of the settings tree (ADR 0004): its file and schema under org/jmouse/cad/settings/pcb/artwork. */
    public static final SettingsSection<Settings> SECTION = new SettingsSection<>("pcb/artwork", "PCB artwork", SettingsOwner.ENGINE_DATA, Settings.class);

    private static final String SILKSCREEN  = "silkscreen";
    private static final String FABRICATION = "fabrication";
    private static final String MASK        = "mask";
    private static final String PASTE       = "paste";
    private static final String ADHESIVE    = "adhesive";

    private static final PathParser PATHS = new PathParser();

    /**
     * The numbers no document carries.
     *
     * @param maskExpansion   how far the mask's opening reaches past a pad all round
     * @param silkscreenWidth the width a silkscreen outline is drawn with when it names none
     * @param letterStroke    how thick a letter's stroke is, as a share of its height
     * @param curveSteps      how many straight pieces a curve is drawn with where a format has no curve
     * @param cornerSteps     how many straight pieces a quarter of a pad's rounded corner is drawn with
     * @param arcError        how far a round offset followed in straight pieces may stand off the true curve: what a pour
     *                        keeps its gap by, the gap grown by it so no copper is poured nearer than the rule
     * @param designator      where and how large a part's designator is lettered
     * @param teardrops       how the drops tracks are widened with are shaped
     * @param checks          how the pcb's checks measure
     * @param areas           how the pcb as made is drawn as areas for an editor and its 3D view
     */
    public record Settings(double maskExpansion, double silkscreenWidth, double letterStroke, int curveSteps, int cornerSteps, double arcError,
                           PlacedDesignator.Rules designator, Teardrops.Shape teardrops, Checks checks, Areas areas) {

        /** The checks' own settings, the rest theirs as the artwork's. */
        public PcbChecks.Settings forChecks() {
            return new PcbChecks.Settings(checks.tolerance(), checks.namedGaps(), curveSteps, designator, teardrops);
        }
    }

    /**
     * @param tolerance how much nearer than a rule a value may be and still pass, in millimetres
     * @param namedGaps how many narrow gaps are named one by one
     */
    public record Checks(double tolerance, int namedGaps) {
    }

    /**
     * @param steps     how many straight steps a quarter of a round end, a disc, an arc or a drill is drawn with
     * @param tolerance how far an area's outline may move when it is simplified for a view, in millimetres
     */
    public record Areas(int steps, double tolerance) {
    }

    private final PcbFabrication         fabrication;
    private final Settings                 settings;
    private final StrokeFont               font;
    private final PadOutline               outlines;
    private final CopperNets               nets;
    private final List<Optional<Geometry>> pours;
    private final List<Teardrops.Drop>     drops;

    private PcbArtwork(PcbFabrication fabrication, Settings settings, StrokeFont font, PadOutline outlines,
                         CopperNets nets, List<Optional<Geometry>> pours, List<Teardrops.Drop> drops) {
        this.fabrication = fabrication;
        this.settings = settings;
        this.font = font;
        this.outlines = outlines;
        this.nets = nets;
        this.pours = pours;
        this.drops = drops;
    }

    /**
     * The pcb's artwork: its nets worked out, its zones poured and its drops made once, for every layer.
     *
     * @param rules the values of the rules the pcb is made by: the gaps a pour keeps
     * @param font  the silkscreen's letters
     */
    public static PcbArtwork of(PcbFabrication fabrication, Map<String, Double> rules, Settings settings, StrokeFont font) {
        PcbDrawing pcb = fabrication.pcb();
        PadOutline outlines = new PadOutline(settings.curveSteps(), settings.cornerSteps());
        CopperNets nets = fabrication.copperNets(outlines, settings.curveSteps());
        List<Optional<Geometry>> pours = new ZonePours(pcb, nets, rules, settings.curveSteps(), settings.arcError()).pour();
        List<Teardrops.Drop> drops = Teardrops.of(pcb, nets.pads(), settings.teardrops());
        return new PcbArtwork(fabrication, settings, font, outlines, nets, pours, drops);
    }

    public PcbFabrication fabrication() {
        return fabrication;
    }

    public Settings settings() {
        return settings;
    }

    /** How a pad's outline is drawn, as the artwork draws it. */
    public PadOutline outlines() {
        return outlines;
    }

    /** Which copper is on which net. */
    public CopperNets nets() {
        return nets;
    }

    /** Every zone poured, by its place in the pcb's list; empty where nothing of it is left. */
    public List<Optional<Geometry>> pours() {
        return pours;
    }

    /** Every drop the pcb's tracks are widened with. */
    public List<Teardrops.Drop> drops() {
        return drops;
    }

    /** What the pcb's checks find, held to these values, on the copper as this artwork worked it out. */
    public List<PcbFinding> checks(Map<String, Double> values) {
        return fabrication.checks(settings, values, nets);
    }

    /**
     * One copper layer: its zones as poured first, the pads with copper on it, a footprint's own copper drawing, the
     * tracks — a rounded corner as the arc it is —, the pcb's arcs, the vias that reach it, the rings of plated mounting
     * holes, and the drops its tracks are widened with. Each piece of copper carries its net, as the pcb's connections
     * give it: a zone its own, a pad its pin's, a track, an arc, a via and a drop the net they join.
     */
    public List<ArtworkFeature> copper(String layer) {
        PcbDrawing pcb = fabrication.pcb();
        List<ArtworkFeature> features = new ArrayList<>();
        for (int index = 0; index < pours.size(); index++) {
            if (pcb.zones().get(index).layer().equals(layer)) {
                String net = pcb.zones().get(index).net();
                pours.get(index).ifPresent(pour -> features.addAll(regionsOf(pour, net)));
            }
        }
        features.addAll(shapesOn(layer, Role.CONDUCTOR));
        for (PcbFabrication.SeatedPart part : fabrication.parts()) {
            FootprintDrawing drawing = part.drawing().orElse(null);
            if (drawing == null) {
                continue;
            }
            for (FootprintDrawing.Pad pad : drawing.pads()) {
                if (pad.hasCopperOn(layer, part.placement())) {
                    features.add(new ArtworkFeature.Pad(part.placement(), pad, 0, pad.isSurface() ? Role.SURFACE_PAD : Role.COMPONENT_PAD,
                            part.padNets().get(pad.number())));
                }
            }
            // A footprint's copper drawing — a logo, a shield — is copper of its side as it is drawn.
            for (FootprintDrawing.Outline outline : drawing.outlines()) {
                if ("copper".equals(outline.layer()) && part.placement().side().equals(layer)) {
                    features.addAll(outlineOf(part.placement(), outline, outline.width(), Role.CONDUCTOR));
                }
            }
        }
        for (int index = 0; index < pcb.tracks().size(); index++) {
            PcbDrawing.Track track = pcb.tracks().get(index);
            if (track.layer().equals(layer)) {
                features.addAll(trackOf(track, nets.trackNets().get(index).orElse(null)));
            }
        }
        for (int index = 0; index < pcb.arcs().size(); index++) {
            PcbDrawing.Arc arc = pcb.arcs().get(index);
            if (arc.layer().equals(layer)) {
                features.add(new ArtworkFeature.Arc(arc.from(), arc.through(), arc.to(), arc.width(), Role.CONDUCTOR,
                        nets.arcNets().get(index).orElse(null)));
            }
        }
        for (int index = 0; index < pcb.vias().size(); index++) {
            PcbDrawing.Via via = pcb.vias().get(index);
            if (pcb.reaches(via, layer)) {
                features.add(new ArtworkFeature.Disc(new PcbDrawing.Point(via.x(), via.y()), via.diameter(), Role.VIA_PAD,
                        nets.viaNets().get(index).orElse(null)));
            }
        }
        // A plated mounting hole's ring, on every copper layer of the stack.
        for (PcbDrawing.Hole hole : pcb.holes()) {
            if (hole.ring() > 0 && pcb.copperLayerIds().contains(layer)) {
                features.add(new ArtworkFeature.Disc(new PcbDrawing.Point(hole.x(), hole.y()), hole.ring(), Role.COMPONENT_PAD, null));
            }
        }
        for (Teardrops.Drop drop : drops) {
            if (drop.layer().equals(layer)) {
                features.add(new ArtworkFeature.Region(drop.outline(), List.of(), Role.CONDUCTOR,
                        nets.trackNets().get(drop.track()).orElse(null)));
            }
        }
        return features;
    }

    /**
     * Where a side's solder mask is left open: every pad with copper on the side but an unplated hole, and every plated
     * mounting hole's ring, each grown by its own margin or the mask's expansion, each via that says so, the copper left bare, and what
     * was drawn on it.
     */
    public List<ArtworkFeature> maskOpenings(String side) {
        List<ArtworkFeature> features = new ArrayList<>(pads(side, pad -> pad.maskMargin() != null ? pad.maskMargin() : settings.maskExpansion(), FootprintDrawing.Pad::opensMask));
        for (PcbDrawing.Via via : fabrication.pcb().vias()) {
            if (via.opensMaskOn(side)) {
                features.add(new ArtworkFeature.Disc(new PcbDrawing.Point(via.x(), via.y()),
                        via.diameter() + settings.maskExpansion() * 2, Role.OPENING, null));
            }
        }
        for (PcbDrawing.Hole hole : fabrication.pcb().holes()) {
            if (hole.ring() > 0) {
                features.add(new ArtworkFeature.Disc(new PcbDrawing.Point(hole.x(), hole.y()),
                        hole.ring() + settings.maskExpansion() * 2, Role.OPENING, null));
            }
        }
        features.addAll(maskWindows(side));
        return features;
    }

    /**
     * The windows in a side's mask that are no pad's, via's or hole's: over the copper left bare, and what is drawn on the mask
     * itself — where an editor shows the copper the finish covers.
     */
    public List<ArtworkFeature> maskWindows(String side) {
        List<ArtworkFeature> features = new ArrayList<>(bareCopperOn(side));
        features.addAll(drawnBesides(side, MASK, Role.OPENING));
        return features;
    }

    /** The openings over a side's bare copper: its tracks, arcs and pours, each grown by the mask's expansion. */
    private List<ArtworkFeature> bareCopperOn(String side) {
        PcbDrawing pcb = fabrication.pcb();
        double growth = settings.maskExpansion() * 2;
        List<ArtworkFeature> features = new ArrayList<>();
        for (PcbDrawing.Track track : pcb.tracks()) {
            if (track.bare() && track.layer().equals(side)) {
                trackOf(new PcbDrawing.Track(track.layer(), track.width() + growth, track.rounding(), track.points()), null).stream().map(PcbArtwork::asOpening).forEach(features::add);
            }
        }
        for (PcbDrawing.Arc arc : pcb.arcs()) {
            if (arc.bare() && arc.layer().equals(side)) {
                features.add(new ArtworkFeature.Arc(arc.from(), arc.through(), arc.to(), arc.width() + growth, Role.OPENING, null));
            }
        }
        for (int index = 0; index < pours.size(); index++) {
            PcbDrawing.Zone zone = pcb.zones().get(index);
            if (zone.bare() && zone.layer().equals(side)) {
                pours.get(index).ifPresent(pour -> regionsOf(pour.buffer(settings.maskExpansion()), null).stream().map(PcbArtwork::asOpening).forEach(features::add));
            }
        }
        return features;
    }

    /** A piece of copper's shape, as what the mask leaves open. */
    private static ArtworkFeature asOpening(ArtworkFeature feature) {
        return switch (feature) {
            case ArtworkFeature.Stroke stroke -> new ArtworkFeature.Stroke(stroke.points(), stroke.width(), Role.OPENING, null);
            case ArtworkFeature.Arc arc -> new ArtworkFeature.Arc(arc.from(), arc.through(), arc.to(), arc.width(), Role.OPENING, null);
            case ArtworkFeature.Region region -> new ArtworkFeature.Region(region.outline(), region.holes(), Role.OPENING, null);
            default -> feature;
        };
    }

    /** A side's paste: every surface pad on the side, as it is, and what was drawn on the paste itself. */
    public List<ArtworkFeature> paste(String side) {
        List<ArtworkFeature> features = new ArrayList<>(pads(side, pad -> pad.pasteMargin() != null ? pad.pasteMargin() : 0, FootprintDrawing.Pad::opensPaste));
        features.addAll(drawnBesides(side, PASTE, Role.OPENING));
        return features;
    }

    /** A side's glue: only what is drawn on it. */
    public List<ArtworkFeature> adhesive(String side) {
        return drawnBesides(side, ADHESIVE, Role.LEGEND);
    }

    /** One side's assembly drawing: its parts' fabrication outlines, each part's designator, and what the pcb draws on it. */
    public List<ArtworkFeature> assembly(String side) {
        List<ArtworkFeature> features = new ArrayList<>(drawnBesides(side, FABRICATION, Role.LEGEND));
        features.addAll(designators(side, standing -> true));
        return features;
    }

    /** What the side's parts' footprints and the pcb itself draw on a layer of the side: outlines, shapes and words. */
    private List<ArtworkFeature> drawnBesides(String side, String purpose, Role role) {
        List<ArtworkFeature> features = new ArrayList<>();
        for (PcbFabrication.SeatedPart part : fabrication.parts()) {
            if (!part.placement().side().equals(side)) {
                continue;
            }
            for (FootprintDrawing.Outline outline : part.drawing().map(FootprintDrawing::outlines).orElse(List.of())) {
                if (purpose.equals(outline.layer())) {
                    features.addAll(outlineOf(part.placement(), outline,
                            outline.width() > 0 ? outline.width() : settings.silkscreenWidth(), role));
                }
            }
        }
        String layer = PcbDrawing.layerOf(side, purpose);
        features.addAll(shapesOn(layer, role));
        for (PcbDrawing.Text words : fabrication.pcb().texts()) {
            if (words.layer().equals(layer)) {
                features.addAll(lettered(words.text(), words.x(), words.y(), words.size(), words.rotation(), side, anchorOf(words.anchor())));
            }
        }
        return features;
    }

    /** The shapes the pcb itself draws on a layer: an area, or lines with a round pen. */
    private List<ArtworkFeature> shapesOn(String layer, Role role) {
        List<ArtworkFeature> features = new ArrayList<>();
        for (PcbDrawing.Shape shape : fabrication.pcb().shapes()) {
            if (!shape.layer().equals(layer)) {
                continue;
            }
            String net = shape.net().isEmpty() ? null : shape.net();
            for (List<double[]> run : runs(shape.path())) {
                List<PcbDrawing.Point> points = run.stream().map(point -> new PcbDrawing.Point(point[0], point[1])).toList();
                features.add(shape.filled() ? new ArtworkFeature.Region(points, List.of(), role, net)
                        : new ArtworkFeature.Stroke(points, shape.width() > 0 ? shape.width() : settings.silkscreenWidth(), role, net));
            }
        }
        return features;
    }

    /** The side's parts' designators that are taken, each lettered where the pcb shows it. */
    private List<ArtworkFeature> designators(String side, Predicate<PlacedDesignator> taken) {
        List<ArtworkFeature> features = new ArrayList<>();
        for (PcbFabrication.SeatedPart part : fabrication.parts()) {
            if (part.placement().side().equals(side)) {
                PlacedDesignator standing = PlacedDesignator.of(part.placement(), part.drawing(), settings.designator());
                if (taken.test(standing)) {
                    features.addAll(lettered(part.printed(), standing.x(), standing.y(), standing.height(), standing.rotation(), side, anchorOf(standing.anchor())));
                }
            }
        }
        return features;
    }

    /**
     * One side's silkscreen: its parts' silkscreen outlines, each part's designator lettered where the pcb shows it,
     * and the pcb's own words on that side — read from above on the top and mirrored on the bottom, as the pcb is
     * seen from its own side.
     */
    public List<ArtworkFeature> silkscreen(String side) {
        List<ArtworkFeature> features = new ArrayList<>(drawnBesides(side, SILKSCREEN, Role.LEGEND));
        features.addAll(designators(side, PlacedDesignator::printed));
        return features;
    }

    /** The runs a path draws, each of at least two points, a curve in the artwork's straight pieces. */
    public List<List<double[]>> runs(String path) {
        if (path == null || path.isBlank()) {
            return List.of();
        }
        return PathPolylines.of(PATHS.parse(path), settings.curveSteps()).stream().filter(run -> run.size() > 1).toList();
    }

    private List<ArtworkFeature> pads(String side, ToDoubleFunction<FootprintDrawing.Pad> growth, Predicate<FootprintDrawing.Pad> taken) {
        List<ArtworkFeature> features = new ArrayList<>();
        for (PcbFabrication.SeatedPart part : fabrication.parts()) {
            for (FootprintDrawing.Pad pad : part.drawing().map(FootprintDrawing::pads).orElse(List.of())) {
                if (pad.hasCopperOn(side, part.placement()) && taken.test(pad)) {
                    features.add(new ArtworkFeature.Pad(part.placement(), pad, growth.applyAsDouble(pad), Role.OPENING, null));
                }
            }
        }
        return features;
    }

    /** A footprint's drawing placed with its part: filled as an area, or stroked with a round pen of {@code width}. */
    private List<ArtworkFeature> outlineOf(PcbDrawing.Placement placement, FootprintDrawing.Outline outline, double width, Role role) {
        List<ArtworkFeature> features = new ArrayList<>();
        for (List<double[]> run : runs(outline.path())) {
            List<PcbDrawing.Point> placed = run.stream().map(point -> placement.placed(point[0], point[1])).toList();
            features.add(outline.filled() ? new ArtworkFeature.Region(placed, List.of(), role, null)
                    : new ArtworkFeature.Stroke(placed, width, role, null));
        }
        return features;
    }

    /** Which part of the letters stands on their point, as the stroke font says it. */
    private static StrokeText.Anchor anchorOf(String anchor) {
        return switch (anchor) {
            case PcbDrawing.START -> StrokeText.Anchor.START;
            case PcbDrawing.END -> StrokeText.Anchor.END;
            default -> StrokeText.Anchor.MIDDLE;
        };
    }

    private List<ArtworkFeature> lettered(String words, double x, double y, double height, double rotation, String side, StrokeText.Anchor anchor) {
        StrokeText laid = StrokeText.lay(font, words, height, anchor, StrokeText.Baseline.MIDDLE, settings.curveSteps());
        double angle = Math.toRadians(rotation);
        double mirror = PcbDrawing.BOTTOM.equals(side) ? -1 : 1;
        List<ArtworkFeature> features = new ArrayList<>();
        for (List<StrokeText.Point> stroke : laid.strokes()) {
            if (stroke.size() < 2) {
                continue;
            }
            features.add(new ArtworkFeature.Stroke(stroke.stream().map(point -> {
                double across = point.x() * mirror;
                return new PcbDrawing.Point(x + across * Math.cos(angle) + point.y() * Math.sin(angle),
                        y - across * Math.sin(angle) + point.y() * Math.cos(angle));
            }).toList(), height * settings.letterStroke(), Role.LEGEND, null));
        }
        return features;
    }

    /** A track as its copper runs ({@link RoundedTrack}): each run a stroke, each rounded corner an arc. */
    private static List<ArtworkFeature> trackOf(PcbDrawing.Track track, String net) {
        return RoundedTrack.of(track).stream().<ArtworkFeature>map(piece -> switch (piece) {
            case RoundedTrack.Run run -> new ArtworkFeature.Stroke(run.points(), track.width(), Role.CONDUCTOR, net);
            case RoundedTrack.Bend bend -> new ArtworkFeature.Arc(bend.from(), bend.through(), bend.to(), track.width(), Role.CONDUCTOR, net);
        }).toList();
    }

    /** A zone's copper: each polygon an area less its holes, the largest first, as the pour orders them, on the zone's net. */
    private static List<ArtworkFeature> regionsOf(Geometry pour, String net) {
        List<ArtworkFeature> regions = new ArrayList<>();
        for (Polygon polygon : ZonePours.polygonsOf(pour)) {
            List<List<PcbDrawing.Point>> holes = new ArrayList<>();
            for (int hole = 0; hole < polygon.getNumInteriorRing(); hole++) {
                holes.add(ring(polygon.getInteriorRingN(hole)));
            }
            regions.add(new ArtworkFeature.Region(ring(polygon.getExteriorRing()), holes, Role.CONDUCTOR, net));
        }
        return regions;
    }

    private static List<PcbDrawing.Point> ring(LineString ring) {
        return Arrays.stream(ring.getCoordinates()).map((Coordinate point) -> new PcbDrawing.Point(point.x, point.y)).toList();
    }
}
