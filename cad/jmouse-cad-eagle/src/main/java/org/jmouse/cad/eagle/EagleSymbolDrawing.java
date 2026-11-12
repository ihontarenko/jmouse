package org.jmouse.cad.eagle;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.exchange.imported.SymbolCanvas;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.eagle.EagleGraphic;
import org.jmouse.eagle.EagleLibrary;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * An Eagle symbol as a symbol document, built as its tree: its wires, arcs, circles, rectangles and polygons as path
 * data, its pins where Eagle connects them — the line reaching from there, a dot for an inverted one —, its name and value
 * where Eagle writes {@code >NAME} and {@code >VALUE}.
 *
 * <p>Eagle draws a symbol in millimetres with Y upwards on a 0.1 inch grid, twice the 1.27 mm one a sheet is laid out
 * on, so a pin on Eagle's grid is a pin on ours. A drawing stands round the middle of its body; Eagle's origin is often
 * at a corner, so the drawing is moved by {@link #centreOf} — and a part placed with it by the same, turned.</p>
 */
final class EagleSymbolDrawing {

    static final String NAME = ">NAME";
    static final String VALUE = ">VALUE";

    private EagleSymbolDrawing() {
    }

    /** The symbol's document: what it is called and whose it is, and its drawing. */
    static DocumentNode documentOf(String library, EagleLibrary.Symbol symbol, EagleLibraries filing) {
        DocumentNode document = new DocumentNode(SchematicModule.SYMBOL, symbol.name(), EagleNames.symbol(library, symbol.name()),
                null, 1);
        Provenance provenance = filing.provenance(EagleNames.symbolSource(library, symbol.name()), false);
        MetaNode meta = MetaNode.filed(library + ":" + symbol.name() + ", drawn as Eagle draws it", EagleNames.symbolCode(library, symbol.name()),
                filing.libraryCategory(provenance),
                filing.symbolTags());
        provenance.writeInto(meta);
        document.addExpression(meta);
        double[] centre = centreOf(symbol);
        SymbolCanvas canvas = new SymbolCanvas(centre[0], centre[1]);
        draw(symbol, canvas, true);
        canvas.lines().forEach(document::addExpression);
        return document;
    }

    /** Where the symbol's body and pins are centred, in drawing units from Eagle's origin, Y downwards, on the grid. */
    static double[] centreOf(EagleLibrary.Symbol symbol) {
        SymbolCanvas measured = new SymbolCanvas(0, 0);
        draw(symbol, measured, false);
        return measured.centre();
    }

    /** Its shapes and its pins' lines; its pins and labels too where {@code named}, as a drawing names them. */
    private static void draw(EagleLibrary.Symbol symbol, SymbolCanvas canvas, boolean named) {
        BiFunction<Double, Double, double[]> place = (x, y) -> point(canvas, x, y);
        for (EagleGraphic graphic : symbol.graphics()) {
            switch (graphic) {
                case EagleGraphic.Wire wire -> EagleGeometry.wire(wire, canvas.path(SymbolCanvas.Fill.STROKE), place);
                case EagleGraphic.Circle circle -> {
                    double[] centre = point(canvas, circle.x(), circle.y());
                    double radius = circle.radius() * SymbolCanvas.UNITS_PER_MILLIMETRE;
                    canvas.reach(centre[0] - radius, centre[1] - radius);
                    canvas.reach(centre[0] + radius, centre[1] + radius);
                    // A circle Eagle draws with no width is filled.
                    SymbolCanvas.Fill fill = circle.width() == 0 ? SymbolCanvas.Fill.SOLID : SymbolCanvas.Fill.STROKE;
                    canvas.path(fill).circle(centre[0], centre[1], radius);
                }
                case EagleGraphic.Rectangle rectangle -> EagleGeometry.rectangle(rectangle, canvas.path(SymbolCanvas.Fill.SOLID), place);
                case EagleGraphic.Polygon polygon -> EagleGeometry.polygon(polygon, canvas.path(SymbolCanvas.Fill.SOLID), place);
                case EagleGraphic.Text text -> {
                    if (named) {
                        label(text, canvas);
                    }
                }
            }
        }
        Map<String, String> names = EagleNames.pins(symbol);
        for (EagleLibrary.Pin pin : symbol.pins()) {
            double[] at = point(canvas, pin.x(), pin.y());
            double length = pin.length().millimetres();
            double[] direction = EagleGeometry.direction(pin.rotation());
            double[] body = length > 0 ? point(canvas, pin.x() + direction[0] * length, pin.y() + direction[1] * length) : null;
            if (body != null) {
                canvas.pinLine(at, body, pin.function().inverted());
            }
            if (named) {
                canvas.pin(names.get(pin.name()), at, body, pin.visible().nameShown() ? pin.shownName() : null);
            }
        }
    }

    /** Where Eagle writes the part's name and value: its {@code >NAME} and {@code >VALUE} texts. */
    private static void label(EagleGraphic.Text text, SymbolCanvas canvas) {
        String written = text.text().strip().toUpperCase();
        if (written.equals(NAME)) {
            canvas.label("name", point(canvas, text.x(), text.y()));
        } else if (written.equals(VALUE)) {
            canvas.label("value", point(canvas, text.x(), text.y()));
        }
    }

    /** An Eagle point, millimetres with Y upwards, on the canvas. */
    private static double[] point(SymbolCanvas canvas, double x, double y) {
        return canvas.point(x * SymbolCanvas.UNITS_PER_MILLIMETRE, -y * SymbolCanvas.UNITS_PER_MILLIMETRE);
    }
}
