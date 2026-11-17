package org.jmouse.cad.schematic;

import java.util.Map;
import org.jmouse.cad.document.DocumentModel;
import org.jmouse.cad.schematic.drawing.SymbolDrawing;
import org.jmouse.cad.schematic.drawing.LivingDrawing;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentModule;
import org.jmouse.el.parser.Parser;

import java.util.List;

/**
 * The schematic's documents: what a part is drawn with on a sheet — a {@code symbol}, a {@code portrait} (the part as it
 * looks in the hand), a {@code frame} (a sheet's border and title block). Each is a living drawing: its body is lines and
 * elements said as grammars ({@link DrawingGrammar}), bound to a part's figures by the inputs it declares.
 */
public final class SchematicModule implements DocumentModule {

    public static final DocumentKind SYMBOL   = DocumentKind.of("symbol").drawnBy(DrawingLinesParser.class);
    public static final DocumentKind PORTRAIT = DocumentKind.of("portrait").drawnBy(DrawingLinesParser.class);
    public static final DocumentKind FRAME    = DocumentKind.of("frame").drawnBy(DrawingLinesParser.class);

    @Override
    public List<DocumentKind> kinds() {
        return List.of(SYMBOL, PORTRAIT, FRAME);
    }

    @Override
    public Map<DocumentKind, DocumentModel<?>> models() {
        DocumentModel<SymbolDrawing> drawing = DocumentModel.of(SymbolDrawing.class, LivingDrawing::of);
        return Map.of(SYMBOL, drawing, PORTRAIT, drawing, FRAME, drawing);
    }

    @Override
    public List<Parser> getParsers() {
        return List.of(new DrawingLinesParser());
    }
}
