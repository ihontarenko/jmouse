package org.jmouse.cad.eagle;

import java.util.Locale;
import org.jmouse.cad.exchange.imported.ImportedFields;
import org.jmouse.cad.pcb.FootprintLines;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.imported.FootprintOutlines;
import org.jmouse.cad.exchange.imported.ImportedFootprint;
import org.jmouse.cad.exchange.imported.ImportedSummary;
import org.jmouse.cad.exchange.imported.RoundedPath;
import org.jmouse.eagle.EagleGraphic;
import org.jmouse.eagle.EagleLibrary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * An Eagle package as a footprint document, built as its tree: its pads, surface pads and holes, and the lines it draws
 * as path data — one outline for each layer, width and fill. Eagle draws in millimetres with Y upwards, a footprint
 * with Y downwards: every point is flipped, and nothing scaled; an angle turns the same way on the screen in both.
 *
 * <p>Which of Eagle's layers is ours, the filing says ({@link EagleLibraries#layerOf}): its place, documentation and
 * copper. Its names, values, keepouts and texts are not a part's.</p>
 */
final class EagleFootprintDrawing {

    private static final int PLACES = FootprintOutlines.PLACES;

    /** How much longer than wide Eagle draws a {@code long} or {@code offset} pad. */
    private static final double LONG_PAD = 2;

    /** Eagle's roundness is a percentage of the shorter side's half; our ratio is of the whole shorter side. */
    private static final double ROUNDNESS_SCALE = 0.5 / 100;

    private EagleFootprintDrawing() {
    }

    static DocumentNode documentOf(String library, EagleLibrary.Package found, EagleLibraries filing) {
        String said = found.description().isBlank() ? library + ":" + found.name() : ImportedSummary.ofHtml(found.description());
        Map<String, String> numbers = EagleNames.pads(found);
        List<FootprintLines.Pad> pads = new ArrayList<>();
        found.pads().forEach(pad -> pads.add(pad(pad, numbers.get(pad.name()), filing)));
        found.smds().forEach(smd -> pads.add(smd(smd, numbers.get(smd.name()))));
        found.holes().forEach(hole -> pads.add(hole(hole)));
        return new ImportedFootprint(found.name(), EagleNames.footprint(library, found.name()), EagleNames.footprintCode(library, found.name()),
                said + " — imported from Eagle",
                filing.provenance(EagleNames.packageSource(library, found.name()), false), pads,
                outlinesOf(found.graphics(), filing), fieldsOf(found.graphics(), filing), List.of()).document(filing);
    }

    /** A through-hole pad: its ring as Eagle draws its shape, or as the design rules give it where it leaves it to them. */
    private static FootprintLines.Pad pad(EagleLibrary.Pad pad, String number, EagleLibraries filing) {
        RoundedPath round = new RoundedPath(PLACES);
        double diameter = pad.diameter() > 0 ? pad.diameter() : filing.ringedDiameter(pad.drill());
        boolean elongated = pad.shape().elongated();
        String shape = switch (pad.shape()) {
            case SQUARE -> "rectangle";
            case LONG, OFFSET -> "oval";
            // An octagon is a round ring as far as a pcb's copper goes: its corners are a tenth of its width.
            case ROUND, OCTAGON -> "circle";
        };
        return new FootprintLines.Pad(number, "through-hole", shape, round.rounded(pad.x()), round.rounded(-pad.y()),
                round.rounded(pad.rotation().angle()), round.rounded(elongated ? LONG_PAD * diameter : diameter),
                round.rounded(diameter), round.rounded(pad.drill()), round.rounded(pad.drill()), 0, Optional.empty());
    }

    private static FootprintLines.Pad smd(EagleLibrary.Smd smd, String number) {
        RoundedPath round = new RoundedPath(PLACES);
        String shape = smd.roundness() > 0 ? "rounded-rectangle" : "rectangle";
        return new FootprintLines.Pad(number, "surface", shape, round.rounded(smd.x()), round.rounded(-smd.y()),
                round.rounded(smd.rotation().angle()), round.rounded(smd.dx()), round.rounded(smd.dy()), 0, 0,
                round.rounded(smd.roundness() * ROUNDNESS_SCALE), Optional.empty());
    }

    /** A hole with no copper round it, numbered by nothing: no pin lands on it. */
    private static FootprintLines.Pad hole(EagleLibrary.Hole hole) {
        RoundedPath round = new RoundedPath(PLACES);
        double drill = round.rounded(hole.drill());
        return new FootprintLines.Pad("", "unplated-hole", "circle", round.rounded(hole.x()), round.rounded(-hole.y()), 0, drill,
                drill, drill, drill, 0, Optional.empty());
    }

    /**
     * Where the placed part's name and value are printed: Eagle's {@code >NAME} and {@code >VALUE} texts, placed by where their
     * letters begin ({@link ImportedFields}), on the layer Eagle prints them on.
     */
    private static List<FootprintLines.Field> fieldsOf(List<EagleGraphic> graphics, EagleLibraries filing) {
        List<FootprintLines.Field> fields = new ArrayList<>();
        for (EagleGraphic graphic : graphics) {
            if (graphic instanceof EagleGraphic.Text text) {
                String written = text.text().strip().toUpperCase(Locale.ROOT);
                String subject = written.equals(EagleSymbolDrawing.NAME) ? PcbVocabulary.FIELD_REFERENCE
                        : written.equals(EagleSymbolDrawing.VALUE) ? PcbVocabulary.FIELD_VALUE : null;
                if (subject != null) {
                    String layer = filing.layerOf(String.valueOf(text.layer())).filter(PcbVocabulary.FIELD_LAYERS::contains).orElse(null);
                    fields.add(ImportedFields.begunAt(subject, text.x(), text.y(), text.size(), text.rotation().angle(), layer));
                }
            }
        }
        return fields;
    }

    /** One outline for each layer, width and fill, its shapes in the order Eagle draws them. */
    private static List<FootprintLines.Outline> outlinesOf(List<EagleGraphic> graphics, EagleLibraries filing) {
        FootprintOutlines outlines = new FootprintOutlines();
        BiFunction<Double, Double, double[]> place = (x, y) -> new double[] {x, -y};
        for (EagleGraphic graphic : graphics) {
            Optional<String> layer = filing.layerOf(String.valueOf(graphic.layer()));
            if (layer.isEmpty()) {
                continue;
            }
            switch (graphic) {
                case EagleGraphic.Wire wire -> EagleGeometry.wire(wire, outlines.path(layer.get(), wire.width(), false), place);
                case EagleGraphic.Circle circle -> {
                    boolean filled = circle.width() == 0;
                    outlines.path(layer.get(), circle.width(), filled).circle(circle.x(), -circle.y(), circle.radius());
                }
                case EagleGraphic.Rectangle rectangle -> EagleGeometry.rectangle(rectangle, outlines.path(layer.get(), 0, true), place);
                case EagleGraphic.Polygon polygon -> EagleGeometry.polygon(polygon,
                        outlines.path(layer.get(), polygon.width(), true), place);
                case EagleGraphic.Text ignored -> {
                }
            }
        }
        return outlines.outlines();
    }
}
