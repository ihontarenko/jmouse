package org.jmouse.cad.panel;

import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentModule;
import org.jmouse.cad.document.DocumentModel;
import org.jmouse.el.parser.Parser;

import java.util.List;
import java.util.Map;

/** The language of panels: one kind of document, written in {@link PanelLinesParser}'s lines. */
public final class PanelModule implements DocumentModule {

    public static final DocumentKind PANEL = DocumentKind.of("panel").drawnBy(PanelLinesParser.class);

    @Override
    public List<DocumentKind> kinds() {
        return List.of(PANEL);
    }

    @Override
    public Map<DocumentKind, DocumentModel<?>> models() {
        return Map.of(PANEL, DocumentModel.of(PanelDrawing.class, PanelDrawing::of));
    }

    @Override
    public List<Parser> getParsers() {
        return List.of(new PanelLinesParser());
    }
}
