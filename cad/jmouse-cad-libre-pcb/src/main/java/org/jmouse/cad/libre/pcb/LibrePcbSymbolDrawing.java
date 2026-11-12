package org.jmouse.cad.libre.pcb;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.exchange.imported.ImportedNames;
import org.jmouse.cad.exchange.imported.ImportedCircuit;
import org.jmouse.cad.exchange.imported.SymbolCanvas;
import org.jmouse.cad.exchange.imported.UniqueIdentifiers;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.libre.pcb.LibrePcbGraphic;
import org.jmouse.libre.pcb.LibrePcbLayer;
import org.jmouse.libre.pcb.LibrePcbLibrary;
import org.jmouse.libre.pcb.LibrePcbPlaceholder;
import org.jmouse.libre.pcb.LibrePcbPoint;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A symbol of the library as a jM symbol, centred on the grid, as every imported symbol is: LibrePCB draws round its own
 * origin, so a part placed with it is moved by the same {@link #centreOf}.
 */
final class LibrePcbSymbolDrawing {

    private static final String PIN_OPENING = "P";

    private LibrePcbSymbolDrawing() {
    }

    /** @param namedPins the pins whose names a sheet shows, by id: what the components using it say */
    static DocumentNode documentOf(LibrePcbLibrary.Symbol symbol, Set<String> namedPins, LibrePcbFiling filing) {
        DocumentNode document = new DocumentNode(SchematicModule.SYMBOL, symbol.name(), LibrePcbNames.symbol(symbol), null, 1);
        Provenance provenance = filing.provenance(LibrePcbNames.source("symbol", symbol.name(), symbol.id()), symbol.author());
        MetaNode meta = MetaNode.filed(symbol.name() + ", drawn as LibrePCB draws it", LibrePcbNames.symbolCode(symbol), filing.libraryCategory(provenance),
                filing.symbolTags());
        provenance.writeInto(meta);
        document.addExpression(meta);
        double[] centre = centreOf(symbol);
        SymbolCanvas canvas = new SymbolCanvas(centre[0], centre[1]);
        draw(symbol, canvas, namedPins, true);
        canvas.lines().forEach(document::addExpression);
        return document;
    }

    /** Where its body and pins are centred, in drawing units from LibrePCB's origin, Y downwards, on the grid. */
    static double[] centreOf(LibrePcbLibrary.Symbol symbol) {
        SymbolCanvas measured = new SymbolCanvas(0, 0);
        draw(symbol, measured, Set.of(), false);
        return measured.centre();
    }

    /** What each of its pins is called on a component that draws with it, by pin. */
    static Map<String, String> pinNames(LibrePcbLibrary.Symbol symbol) {
        UniqueIdentifiers unique = new UniqueIdentifiers(PIN_OPENING, ImportedCircuit.ID_LONGEST);
        Map<String, String> names = new LinkedHashMap<>();
        symbol.pins().forEach(pin -> names.put(pin.id(), unique.of(ImportedNames.name(pin.name(), PIN_OPENING))));
        return names;
    }

    private static void draw(LibrePcbLibrary.Symbol symbol, SymbolCanvas canvas, Set<String> namedPins, boolean named) {
        for (LibrePcbGraphic graphic : symbol.graphics()) {
            if (LibrePcbLayer.SYMBOL_GRAB_AREA.equals(graphic.layer())) {
                continue;
            }
            switch (graphic) {
                case LibrePcbGraphic.Polygon polygon -> LibrePcbPaths.corners(polygon.vertices(),
                        canvas.path(polygon.filled() ? SymbolCanvas.Fill.SOLID : SymbolCanvas.Fill.STROKE), point -> point(canvas, point));
                case LibrePcbGraphic.Circle circle -> {
                    double[] centre = point(canvas, circle.centre());
                    double radius = circle.diameter() / 2 * SymbolCanvas.UNITS_PER_MILLIMETRE;
                    canvas.reach(centre[0] - radius, centre[1] - radius);
                    canvas.reach(centre[0] + radius, centre[1] + radius);
                    canvas.path(circle.filled() ? SymbolCanvas.Fill.SOLID : SymbolCanvas.Fill.STROKE).circle(centre[0], centre[1], radius);
                }
                case LibrePcbGraphic.Text text -> {
                    if (named) {
                        label(text, canvas);
                    }
                }
            }
        }
        Map<String, String> names = pinNames(symbol);
        for (LibrePcbLibrary.Pin pin : symbol.pins()) {
            double[] at = point(canvas, pin.at());
            double radians = Math.toRadians(pin.rotation());
            LibrePcbPoint inside = new LibrePcbPoint(pin.at().x() + Math.cos(radians) * pin.length(), pin.at().y() + Math.sin(radians) * pin.length());
            if (pin.length() > 0) {
                canvas.pinLine(at, point(canvas, inside), false);
            }
            if (named) {
                canvas.pin(names.get(pin.id()), at, pin.length() > 0 ? point(canvas, inside) : null, namedPins.contains(pin.id()) ? pin.name() : null);
            }
        }
    }

    private static void label(LibrePcbGraphic.Text text, SymbolCanvas canvas) {
        if (LibrePcbPlaceholder.NAME.isAll(text.value())) {
            canvas.label("name", point(canvas, text.at()));
        } else if (LibrePcbPlaceholder.VALUE.isAll(text.value())) {
            canvas.label("value", point(canvas, text.at()));
        }
    }

    /** A LibrePCB point, millimetres with Y upwards, on the canvas. */
    private static double[] point(SymbolCanvas canvas, LibrePcbPoint point) {
        return canvas.point(point.x() * SymbolCanvas.UNITS_PER_MILLIMETRE, -point.y() * SymbolCanvas.UNITS_PER_MILLIMETRE);
    }
}
