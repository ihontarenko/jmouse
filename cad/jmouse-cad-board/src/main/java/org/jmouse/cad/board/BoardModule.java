package org.jmouse.cad.board;

import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentModule;
import org.jmouse.el.parser.Parser;

import java.util.List;

/**
 * The board's documents: a {@code footprint} — how a part sits on a board — and a {@code pcb} — a board laid out for a
 * circuit. Both are drawn: their bodies are lines, each said by a grammar ({@link FootprintLinesParser},
 * {@link BoardLinesParser}).
 */
public final class BoardModule implements DocumentModule {

    public static final DocumentKind FOOTPRINT = DocumentKind.of("footprint").drawnBy(FootprintLinesParser.class);
    public static final DocumentKind PCB       = DocumentKind.of("pcb").drawnBy(BoardLinesParser.class);

    @Override
    public List<DocumentKind> kinds() {
        return List.of(FOOTPRINT, PCB);
    }

    @Override
    public List<Parser> getParsers() {
        return List.of(new FootprintLinesParser(), new BoardLinesParser());
    }
}
