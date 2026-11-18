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


    private static final List<LineGrammar> LINES = List.of(
            LineGrammar.line(PanelWords.BOARD).slot(PanelWords.PCB, WORD).required(PanelWords.AT, POINT).optional(PanelWords.ROTATE, QUANTITY).optional(PanelWords.ACROSS, QUANTITY)
                    .optional(PanelWords.DOWN, QUANTITY).optional(PanelWords.GAP, QUANTITY).including(ArrangementLines.CLAUSES).build(),
            LineGrammar.line(PanelWords.SEPARATE).slot(PanelWords.KIND, WORD, PanelSeparation.words()).build(),
            LineGrammar.line(PanelWords.FRAME).optional(PanelWords.RAIL, QUANTITY).optional(PanelWords.SIDES, NAMES).optional(PanelWords.FIDUCIALS, QUANTITY)
                    .optional(PanelWords.TOOLING, QUANTITY).build(),
            LineGrammar.line(PanelWords.FIDUCIAL).required(PanelWords.AT, POINT).optional(PanelWords.DIAMETER, QUANTITY).optional(PanelWords.OPENING, QUANTITY).build(),
            LineGrammar.line(PanelWords.TOOLING).required(PanelWords.AT, POINT).optional(PanelWords.DRILL, QUANTITY).build(),
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
