package org.jmouse.cad.panel;

import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentModule;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.el.parser.Parser;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** The language of panels: one kind of document, written in {@link PanelLinesParser}'s lines. */
public final class PanelModule implements DocumentModule {

    public static final DocumentKind PANEL = DocumentKind.of("panel").drawnBy(PanelLinesParser.class);

    @Override
    public List<DocumentKind> kinds() {
        return List.of(PANEL);
    }

    @Override
    public Map<DocumentKind, Function<DocumentNode, Object>> models() {
        return Map.of(PANEL, PanelDrawing::of);
    }

    @Override
    public List<Parser> getParsers() {
        return List.of(new PanelLinesParser());
    }
}
