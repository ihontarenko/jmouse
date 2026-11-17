package org.jmouse.cad.pcb;

import java.util.Map;
import org.jmouse.cad.document.DocumentModel;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentModule;
import org.jmouse.el.parser.Parser;

import java.util.List;

/**
 * The pcb's documents: a {@code footprint} — how a part sits on a pcb — and a {@code pcb} — a pcb laid out for a
 * circuit. Both are drawn: their bodies are lines, each said by a grammar ({@link FootprintLinesParser},
 * {@link PcbLinesParser}).
 */
public final class PcbModule implements DocumentModule {

    public static final DocumentKind FOOTPRINT = DocumentKind.of("footprint").drawnBy(FootprintLinesParser.class);
    public static final DocumentKind PCB       = DocumentKind.of("pcb").drawnBy(PcbLinesParser.class);

    @Override
    public List<DocumentKind> kinds() {
        return List.of(FOOTPRINT, PCB);
    }

    @Override
    public Map<DocumentKind, DocumentModel<?>> models() {
        return Map.of(FOOTPRINT, DocumentModel.of(FootprintDrawing.class, FootprintDrawing::of), PCB, DocumentModel.of(PcbDrawing.class, PcbDrawing::of));
    }

    @Override
    public List<Parser> getParsers() {
        return List.of(new FootprintLinesParser(), new PcbLinesParser());
    }
}
