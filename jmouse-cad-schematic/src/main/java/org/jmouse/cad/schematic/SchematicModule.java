package org.jmouse.cad.schematic;

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
    public List<Parser> getParsers() {
        return List.of(new DrawingLinesParser());
    }
}
