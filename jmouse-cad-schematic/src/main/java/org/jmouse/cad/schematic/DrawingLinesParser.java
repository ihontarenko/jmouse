package org.jmouse.cad.schematic;

import org.jmouse.el.grammar.GrammarLinesParser;
import org.jmouse.el.grammar.LineGrammar;
import org.jmouse.el.grammar.LineSet;

import java.util.List;

/** The lines of a drawing's body — a symbol's, a portrait's, a frame's —, each read by its grammar ({@link DrawingGrammar}). */
public class DrawingLinesParser extends GrammarLinesParser {

    @Override
    protected List<LineGrammar> grammars() {
        return List.of(DrawingGrammar.SIZE, DrawingGrammar.PIN, DrawingGrammar.LABEL, DrawingGrammar.INDICATOR, DrawingGrammar.INPUT);
    }

    @Override
    public LineSet lineSet() {
        return DrawingGrammar.body();
    }
}
