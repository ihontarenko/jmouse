package org.jmouse.cad.panel;

import org.jmouse.cad.document.ArrangementLines;
import org.jmouse.el.grammar.GrammarLinesParser;
import org.jmouse.el.grammar.LineGrammar;

import java.util.List;

import static org.jmouse.el.grammar.Values.NAMES;
import static org.jmouse.el.grammar.Values.POINT;
import static org.jmouse.el.grammar.Values.QUANTITY;
import static org.jmouse.el.grammar.Values.WORD;

/**
 * A panel's lines:
 * <pre>
 * board(control-board-pcb, at: 10 10, rotate: 90, across: 2, down: 3, gap: 2)
 * separate(mouse-bites)
 * frame(rail: 5, sides: [top, bottom], fiducials: 3, tooling: 4)
 * fiducial(at: 5 2.5, diameter: 1, opening: 2)
 * tooling(at: 2.5 2.5, drill: 3.2)
 * </pre>
 */
public final class PanelLinesParser extends GrammarLinesParser {

    static final String BOARD    = "board";
    static final String SEPARATE = "separate";
    static final String FRAME    = "frame";
    static final String FIDUCIAL = "fiducial";
    static final String TOOLING  = "tooling";

    private static final List<LineGrammar> LINES = List.of(
            LineGrammar.line(BOARD).slot("pcb", WORD).required("at", POINT).optional("rotate", QUANTITY).optional("across", QUANTITY)
                    .optional("down", QUANTITY).optional("gap", QUANTITY).including(ArrangementLines.CLAUSES).build(),
            LineGrammar.line(SEPARATE).slot("kind", WORD, PanelSeparation.words()).build(),
            LineGrammar.line(FRAME).optional("rail", QUANTITY).optional("sides", NAMES).optional("fiducials", QUANTITY)
                    .optional("tooling", QUANTITY).build(),
            LineGrammar.line(FIDUCIAL).required("at", POINT).optional("diameter", QUANTITY).optional("opening", QUANTITY).build(),
            LineGrammar.line(TOOLING).required("at", POINT).optional("drill", QUANTITY).build(),
            ArrangementLines.GROUP_LINE);

    /** The grammar of a panel's line by its word: what a program makes the line with. */
    static LineGrammar grammar(String word) {
        return LINES.stream().filter(line -> word.equals(line.termWord())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("a panel has no '" + word + "' line"));
    }

    @Override
    protected List<LineGrammar> grammars() {
        return LINES;
    }
}
