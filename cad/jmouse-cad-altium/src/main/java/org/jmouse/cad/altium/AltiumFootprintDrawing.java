package org.jmouse.cad.altium;

import org.jmouse.altium.AltiumMeasures;
import org.jmouse.altium.AltiumPcbLibrary.Text;
import org.jmouse.altium.AltiumPcbLibrary;
import org.jmouse.altium.AltiumPcbLibrary.Arc;
import org.jmouse.altium.AltiumPcbLibrary.Body;
import org.jmouse.altium.AltiumPcbLibrary.Expansion;
import org.jmouse.altium.AltiumPcbLibrary.Fill;
import org.jmouse.altium.AltiumPcbLibrary.Footprint;
import org.jmouse.altium.AltiumPcbLibrary.HoleShape;
import org.jmouse.altium.AltiumPcbLibrary.Pad;
import org.jmouse.altium.AltiumPcbLibrary.Point;
import org.jmouse.altium.AltiumPcbLibrary.Region;
import org.jmouse.altium.AltiumPcbLibrary.Track;
import org.jmouse.altium.AltiumPcbLibrary.Via;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.exchange.imported.ImportedFields;
import org.jmouse.cad.exchange.imported.FootprintOutlines;
import org.jmouse.cad.exchange.imported.ImportedFootprint;
import org.jmouse.cad.exchange.imported.ImportedSummary;
import org.jmouse.cad.exchange.imported.RoundedPath;
import org.jmouse.cad.pcb.FootprintLines;
import org.jmouse.cad.pcb.PcbVocabulary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.jmouse.altium.AltiumPcbLibrary.millimetres;

/**
 * A footprint of an Altium PCB library as a footprint document, built as its tree: its pads, the lines, arcs, fills and
 * areas it draws on the layers that are ours ({@link AltiumLibraries#layerOf(int, Map)}), how high its body stands, and the
 * 3D models it embeds. Altium draws with Y upwards, a footprint with Y downwards: every point is flipped; an angle turns
 * the same way on the screen in both.
 *
 * <p>A pad keeps Altium's shape where ours has it. An octagon is a round ring as far as copper goes, as Eagle's import
 * reads it; a rounded rectangle's corner is a share of half the shorter side in Altium, of the whole side in ours.</p>
 */
final class AltiumFootprintDrawing {

    private static final int PLACES = FootprintOutlines.PLACES;

    /** Altium's corner is a percentage of half the shorter side; our roundness is a share of the whole shorter side. */
    private static final double ROUNDNESS_SCALE = 0.5 / 100;

    /** What a footprint's body height is read from where its parameters give none: the tallest body it draws. */
    private static final String OVERALL_HEIGHT = "OVERALLHEIGHT";

    /** A region of copper, of its kinds; the rest — cutouts, board cutouts, cavities — is no drawing of ours. */
    private static final int COPPER_REGION = 0;

    /** One footprint converted, the models it names, and what of it was not carried. */
    record Converted(DocumentNode document, List<String> leftOut) {
    }

    private AltiumFootprintDrawing() {
    }

    /**
     * @param library    the library's file name, {@code rcl.PcbLib}: the namespace of the footprint's code and its source
     * @param kinds      the kind of each of the library's mechanical layers
     * @param models     the code each embedded model the library holds is named by, by its identifier
     */
    static Converted documentOf(String library, Footprint found, Map<Integer, Integer> kinds, Map<String, String> models,
                                AltiumLibraries filing) {
        Map<String, Integer> leftOut = new TreeMap<>(found.leftOut());
        List<FootprintLines.Pad> pads = new ArrayList<>();
        found.pads().forEach(pad -> pads.add(pad(pad, filing)));
        found.vias().forEach(via -> pads.add(via(via)));
        String said = found.description().isBlank() ? library + ":" + found.name() : ImportedSummary.shortened(found.description());
        Provenance whose = filing.provenance(AltiumNames.footprintSource(library, found.name()), false);
        DocumentNode document = new ImportedFootprint(found.name(), AltiumNames.footprint(library, found.name()),
                AltiumNames.footprintCode(library, found.name()), said + " — imported from Altium", whose, pads,
                outlinesOf(found, kinds, filing, leftOut), fieldsOf(found, kinds, filing, leftOut), List.of()).document(filing);
        double height = heightOf(found);
        if (height > 0) {
            document.addExpression(FootprintLines.body(new RoundedPath(PLACES).rounded(height), List.of()));
        }
        for (Body body : found.bodies()) {
            Optional<String> model = body.modelIdentifier().map(identifier -> models.get(identifier.toUpperCase()));
            if (model.isPresent()) {
                document.addExpression(FootprintLines.model(placedModel(model.get(), body), List.of()));
            } else if (body.modelIdentifier().isPresent()) {
                leftOut.merge("3D model the library does not embed", 1, Integer::sum);
            }
        }
        List<String> notes = new ArrayList<>();
        leftOut.forEach((what, count) -> notes.add(found.name() + ": " + count + " × " + what + " not carried"));
        return new Converted(document, notes);
    }

    /** A pad: through the board, on the near side or on the far one; its shape, its hole and its own openings. */
    private static FootprintLines.Pad pad(Pad pad, AltiumLibraries filing) {
        RoundedPath round = new RoundedPath(PLACES);
        double width = millimetres(pad.width());
        double height = millimetres(pad.height());
        boolean through = pad.layer() == filing.multiLayer() && pad.hole() > 0;
        String type = !through ? PcbVocabulary.SURFACE : pad.plated() ? PcbVocabulary.THROUGH_HOLE : PcbVocabulary.UNPLATED_HOLE;
        double hole = millimetres(pad.hole());
        boolean slot = through && pad.holeShape() == HoleShape.SLOT && pad.slotLength() > pad.hole();
        double drillWidth = through ? (slot ? millimetres(pad.slotLength()) : hole) : 0;
        double drillHeight = through ? hole : 0;
        String shape = switch (pad.shape()) {
            case RECTANGLE -> PcbVocabulary.RECTANGLE;
            case ROUNDED_RECTANGLE -> PcbVocabulary.ROUNDED_RECTANGLE;
            case ROUND, OCTAGON -> Math.abs(width - height) < 1e-6 ? PcbVocabulary.CIRCLE : PcbVocabulary.OVAL;
        };
        double roundness = pad.shape() == AltiumPcbLibrary.PadShape.ROUNDED_RECTANGLE ? round.rounded(pad.cornerPercent() * ROUNDNESS_SCALE) : 0;
        String face = !through && pad.layer() == filing.bottomLayer() ? PcbVocabulary.FACE_FAR : "";
        Double maskMargin = pad.maskExpansion() == Expansion.MANUAL ? round.rounded(millimetres(pad.maskMargin())) : null;
        Double pasteMargin = pad.pasteExpansion() == Expansion.MANUAL ? round.rounded(millimetres(pad.pasteMargin())) : null;
        return new FootprintLines.Pad(pad.name(), type, shape, round.rounded(millimetres(pad.at().x())), round.rounded(-millimetres(pad.at().y())),
                round.rounded(pad.rotation()), round.rounded(width), round.rounded(height), round.rounded(drillWidth), round.rounded(drillHeight),
                roundness, Optional.empty(), face, opensOf(pad, through, width, height), maskMargin, pasteMargin);
    }

    /**
     * What a pad leaves open: a pad the mask covers on its side opens no mask; a surface pad whose paste is drawn
     * smaller than nothing — its paste drawn apart, as fills on the paste layer — opens no paste.
     */
    private static String opensOf(Pad pad, boolean through, double width, double height) {
        boolean tented = through ? pad.tentedTop() && pad.tentedBottom() : pad.tentedTop() || pad.tentedBottom();
        boolean noPaste = through || (pad.pasteExpansion() == Expansion.MANUAL && -millimetres(pad.pasteMargin()) >= Math.min(width, height) / 2);
        if (tented) {
            return noPaste ? PcbVocabulary.OPENS_NONE : PcbVocabulary.OPENS_PASTE;
        }
        return noPaste && !through ? PcbVocabulary.OPENS_MASK : "";
    }

    /** A via a footprint carries: a plated hole and its ring, numbered by nothing, as no pin lands on it. */
    private static FootprintLines.Pad via(Via via) {
        RoundedPath round = new RoundedPath(PLACES);
        double hole = round.rounded(millimetres(via.hole()));
        double diameter = round.rounded(millimetres(via.diameter()));
        return new FootprintLines.Pad("", PcbVocabulary.THROUGH_HOLE, PcbVocabulary.CIRCLE, round.rounded(millimetres(via.at().x())),
                round.rounded(-millimetres(via.at().y())), 0, diameter, diameter, hole, hole, 0, Optional.empty());
    }

    /** One outline for each layer, width and fill: tracks and arcs stroked, fills and copper areas filled. */
    private static List<FootprintLines.Outline> outlinesOf(Footprint found, Map<Integer, Integer> kinds, AltiumLibraries filing,
                                                          Map<String, Integer> leftOut) {
        FootprintOutlines outlines = new FootprintOutlines();
        for (Track track : found.tracks()) {
            Optional<String> layer = layerOf(track.layer(), kinds, filing, leftOut);
            layer.ifPresent(ours -> outlines.path(ours, millimetres(track.width()), false)
                    .lineFrom(x(track.start()), y(track.start()), x(track.end()), y(track.end())));
        }
        for (Arc arc : found.arcs()) {
            Optional<String> layer = layerOf(arc.layer(), kinds, filing, leftOut);
            layer.ifPresent(ours -> drawArc(arc, outlines.path(ours, millimetres(arc.width()), false)));
        }
        for (Fill fill : found.fills()) {
            Optional<String> layer = layerOf(fill.layer(), kinds, filing, leftOut);
            layer.ifPresent(ours -> drawFill(fill, outlines.path(ours, 0, true)));
        }
        for (Region region : found.regions()) {
            if (region.kind() != COPPER_REGION) {
                leftOut.merge("area that is no copper (a cutout)", 1, Integer::sum);
                continue;
            }
            Optional<String> layer = layerOf(region.layer(), kinds, filing, leftOut);
            layer.ifPresent(ours -> {
                RoundedPath path = outlines.path(ours, 0, true);
                ring(region.outline(), path);
                region.holes().forEach(hole -> ring(hole, path));
            });
        }
        return outlines.outlines();
    }

    /** What a field says, by the special string Altium writes for it: the placed part's designator, its comment — its value. */
    private static final Map<String, String> FIELD_SUBJECTS = Map.of(Text.DESIGNATOR, PcbVocabulary.FIELD_REFERENCE,
            Text.COMMENT, PcbVocabulary.FIELD_VALUE);

    /**
     * Where the placed part's designator and value stand: each text writing {@code .Designator} or {@code .Comment}, placed by
     * where its letters begin ({@link ImportedFields}). Any other text is not carried.
     */
    private static List<FootprintLines.Field> fieldsOf(Footprint found, Map<Integer, Integer> kinds, AltiumLibraries filing,
                                                       Map<String, Integer> leftOut) {
        List<FootprintLines.Field> fields = new ArrayList<>();
        for (Text text : found.texts()) {
            String subject = FIELD_SUBJECTS.get(text.text());
            if (subject == null) {
                leftOut.merge("text", 1, Integer::sum);
                continue;
            }
            String layer = filing.layerOf(text.layer(), kinds).filter(PcbVocabulary.FIELD_LAYERS::contains).orElse(null);
            fields.add(ImportedFields.begunAt(subject, millimetres(text.at().x()), millimetres(text.at().y()), millimetres(text.height()),
                    text.rotation(), layer));
        }
        return fields;
    }

    private static Optional<String> layerOf(int layer, Map<Integer, Integer> kinds, AltiumLibraries filing, Map<String, Integer> leftOut) {
        Optional<String> ours = filing.layerOf(layer, kinds);
        if (ours.isEmpty()) {
            leftOut.merge("drawing on Altium layer " + layer, 1, Integer::sum);
        }
        return ours;
    }

    /** An arc, or a whole circle where it runs round: counter-clockwise from its start angle to its end, as Altium draws it. */
    private static void drawArc(Arc arc, RoundedPath path) {
        double radius = millimetres(arc.radius());
        double centreX = x(arc.centre());
        double centreY = y(arc.centre());
        double sweep = arc.endAngle() - arc.startAngle();
        while (sweep <= 0) {
            sweep += 360;
        }
        if (sweep >= 360 - 1e-6) {
            path.circle(centreX, centreY, radius);
            return;
        }
        double start = Math.toRadians(arc.startAngle());
        double middle = Math.toRadians(arc.startAngle() + sweep / 2);
        double end = Math.toRadians(arc.startAngle() + sweep);
        // Y flipped: a counter-clockwise angle in Altium is the same turn on the screen, its sine negated.
        path.arcThrough(centreX + radius * Math.cos(start), centreY - radius * Math.sin(start),
                centreX + radius * Math.cos(middle), centreY - radius * Math.sin(middle),
                centreX + radius * Math.cos(end), centreY - radius * Math.sin(end));
    }

    /** A filled rectangle between its corners, turned about its centre. */
    private static void drawFill(Fill fill, RoundedPath path) {
        double left = millimetres(Math.min(fill.first().x(), fill.second().x()));
        double right = millimetres(Math.max(fill.first().x(), fill.second().x()));
        double bottom = millimetres(Math.min(fill.first().y(), fill.second().y()));
        double top = millimetres(Math.max(fill.first().y(), fill.second().y()));
        double centreX = (left + right) / 2;
        double centreY = (bottom + top) / 2;
        double turn = Math.toRadians(fill.rotation());
        double[][] corners = {{left, bottom}, {right, bottom}, {right, top}, {left, top}};
        for (int index = 0; index < corners.length; index++) {
            double dx = corners[index][0] - centreX;
            double dy = corners[index][1] - centreY;
            double turnedX = centreX + dx * Math.cos(turn) - dy * Math.sin(turn);
            double turnedY = centreY + dx * Math.sin(turn) + dy * Math.cos(turn);
            if (index == 0) {
                path.moveTo(turnedX, -turnedY);
            } else {
                path.lineTo(turnedX, -turnedY);
            }
        }
        path.close();
    }

    private static void ring(List<Point> points, RoundedPath path) {
        if (points.size() < 3) {
            return;
        }
        path.moveTo(x(points.getFirst()), y(points.getFirst()));
        points.stream().skip(1).forEach(point -> path.lineTo(x(point), y(point)));
        path.close();
    }

    /**
     * How high the body stands: the height the footprint's parameters give, else the tallest of its bodies — both
     * Altium's units; zero where neither says.
     */
    private static double heightOf(Footprint found) {
        double height = found.height();
        for (Body body : found.bodies()) {
            height = Math.max(height, AltiumMeasures.units(body.properties().get(OVERALL_HEIGHT)));
        }
        return millimetres(height);
    }

    /**
     * A body's model placed as Altium places it: offset by the body's 2D position and lift, turned by its 3D turn and its
     * turn on the board. Our model's offset and turn are written in the convention a KiCad footprint writes them — Y up,
     * each turn the other way round from Altium's —, as KiCad's own Altium import converts them.
     */
    private static FootprintLines.Model placedModel(String code, Body body) {
        RoundedPath round = new RoundedPath(PLACES);
        Map<String, String> properties = body.properties();
        double x = millimetres(AltiumMeasures.units(properties.get("MODEL.2D.X")));
        double y = millimetres(AltiumMeasures.units(properties.get("MODEL.2D.Y")));
        double z = millimetres(AltiumMeasures.units(properties.get("MODEL.3D.DZ")));
        double turnX = AltiumMeasures.degrees(properties.get("MODEL.3D.ROTX"));
        double turnY = AltiumMeasures.degrees(properties.get("MODEL.3D.ROTY"));
        double turnZ = AltiumMeasures.degrees(properties.get("MODEL.3D.ROTZ"));
        double onBoard = AltiumMeasures.degrees(properties.get("MODEL.2D.ROTATION"));
        return new FootprintLines.Model(code, new FootprintLines.Axes(round.rounded(x), round.rounded(y), round.rounded(z)),
                new FootprintLines.Axes(round.rounded(-turnX), round.rounded(-turnY), round.rounded(onBoard - turnZ)),
                FootprintLines.Axes.ONE, false);
    }

    private static double x(Point point) {
        return millimetres(point.x());
    }

    private static double y(Point point) {
        return -millimetres(point.y());
    }
}
