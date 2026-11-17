package org.jmouse.cad.exchange.imported;

import org.jmouse.cad.schematic.drawing.LivingDrawing;
import org.jmouse.cad.schematic.drawing.SymbolDrawing;
import org.jmouse.cad.simulation.CatalogueDocuments;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jmouse.cad.circuit.Circuit;

/**
 * The marks an imported sheet is drawn with — a ground, a net's label, a sheet's port, a global net's label — and the
 * geometry of placing one: where its symbol connects in its own drawing, which way its body goes from there, and the
 * quarter turn that points that body the way the product's own drew it.
 */
public final class SheetMarks {

    /**
     * A mark: what it is on the sheet, where its symbol connects in its own drawing, and which way its body goes from there.
     *
     * @param mark     what the sheet calls it: {@code ground}, {@code power}, {@code label}, {@code port}, {@code global}, {@code entry}
     * @param terminal the pin a wire reaches it by
     * @param bodyX    which way along X its body goes from its pin, in its own drawing: 1 right, -1 left, 0 neither
     * @param bodyY    which way along Y its body goes from its pin: 1 down, -1 up, 0 neither
     */
    public record Shape(String mark, String terminal, double bodyX, double bodyY) {
    }

    public static final Shape GROUND = new Shape("ground", "ground", 0, 1);
    /** A supply rail: its stem goes up from its pin to the bar its name stands over. */
    public static final Shape POWER = new Shape("power", "power", 0, -1);
    public static final Shape LABEL = new Shape("label", "label", 0, -1);
    public static final Shape PORT = new Shape("port", "label", 0, -1);
    public static final Shape GLOBAL = new Shape("global", "label", 0, -1);
    /** A bus entry: its body slants from the wire's end, its pin, back to the bus. */
    public static final Shape ENTRY = new Shape("entry", "entry", -1, -1);

    private static final List<Shape> SHAPES = List.of(GROUND, POWER, LABEL, PORT, GLOBAL, ENTRY);

    /** Where each mark's pin stands in its own drawing, by the mark: the symbol drawing's own, never a number kept here. */
    private final Map<String, double[]> pins;

    private SheetMarks(Map<String, double[]> pins) {
        this.pins = Map.copyOf(pins);
    }

    /**
     * The marks as the documents draw them: each mark's pin read off the symbol the filing draws it with.
     *
     * @param markSymbols the symbol each mark is drawn with, by the mark ({@link ImportFiling#markSymbols})
     * @throws IllegalStateException when a mark's symbol is not among the documents, or places no pin
     */
    public static SheetMarks of(CatalogueDocuments documents, Map<String, String> markSymbols) {
        Map<String, double[]> pins = new LinkedHashMap<>();
        for (Shape shape : SHAPES) {
            String slug = markSymbols.get(shape.mark());
            if (slug == null) {
                throw new IllegalStateException("No symbol is named to draw the mark '" + shape.mark() + "' with.");
            }
            SymbolDrawing drawing = documents.find(slug).map(LivingDrawing::of)
                    .orElseThrow(() -> new IllegalStateException("The mark '" + shape.mark() + "' is drawn with '" + slug
                            + "', which is not among the documents."));
            if (drawing.pins().isEmpty()) {
                throw new IllegalStateException("The mark symbol '" + slug + "' places no pin to connect at.");
            }
            SymbolDrawing.Pin pin = drawing.pins().getFirst();
            pins.put(shape.mark(), new double[] {pin.x(), pin.y()});
        }
        return new SheetMarks(pins);
    }

    /** A point of a symbol on the sheet as jMX turns it: mirrored left to right first, then turned clockwise. */
    public static double[] turned(double[] point, double rotation, boolean mirrored) {
        double radians = Math.toRadians(rotation);
        double localX = mirrored ? -point[0] : point[0];
        return new double[] {localX * Math.cos(radians) - point[1] * Math.sin(radians),
                localX * Math.sin(radians) + point[1] * Math.cos(radians)};
    }

    /** The quarter turn that points a mark's body, as its own drawing goes from its pin, along a direction on the sheet. */
    public static double rotationTowards(double[] direction, Shape shape) {
        double best = 0;
        double bestDot = -Double.MAX_VALUE;
        for (int quarter = 0; quarter < 4; quarter++) {
            double[] turned = turned(new double[] {shape.bodyX(), shape.bodyY()}, quarter * 90, false);
            double dot = turned[0] * direction[0] + turned[1] * direction[1];
            if (dot > bestDot) {
                bestDot = dot;
                best = quarter * 90;
            }
        }
        return best;
    }

    /**
     * A mark whose symbol connects at a point of the sheet and whose body goes one way from it: turned so its body goes
     * that way, moved so its pin stands on the point.
     *
     * @param net  the net it names, or null; a ground of the reference node names none, every sheet's node 0 already
     * @param at   where its pin stands, in drawing units
     * @param body which way its body goes from there
     */
    public ImportedCircuit.Item mark(String id, String net, Shape shape, double[] at, double[] body) {
        double rotation = rotationTowards(body, shape);
        double[] pin = turned(pins.get(shape.mark()), rotation, false);
        String named = shape == GROUND && Circuit.GROUND.equals(net) ? null : net;
        return new ImportedCircuit.Item(id, shape.mark(), named, ImportedCircuit.rounded(at[0] - pin[0]),
                ImportedCircuit.rounded(at[1] - pin[1]), rotation, false);
    }
}
