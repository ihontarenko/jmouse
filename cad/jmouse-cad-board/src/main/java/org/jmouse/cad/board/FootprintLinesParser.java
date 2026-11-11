package org.jmouse.cad.board;

import org.jmouse.el.grammar.GrammarLinesParser;
import org.jmouse.cad.document.DocumentValues;
import org.jmouse.el.grammar.LineGrammar;

import java.util.List;

import static org.jmouse.el.grammar.Values.EXTENT;
import static org.jmouse.el.grammar.Values.EXTENT_OR_QUANTITY;
import static org.jmouse.el.grammar.Values.FLAG;
import static org.jmouse.el.grammar.Values.POINT;
import static org.jmouse.el.grammar.Values.QUANTITY;
import static org.jmouse.el.grammar.Values.TEXT;
import static org.jmouse.el.grammar.Values.WORD;

/**
 * How a part sits on a board, in millimetres: its pads, its outlines, where its reference and value are printed.
 *
 * <pre>
 * pad "1" through-hole rectangle at 0, 0 size 1.905 x 2 drill 1.1
 * pad "2" surface rounded-rectangle at 2.54, 0 rotate 90 size 1 x 1.5 roundness 0.25
 * outline silkscreen width 0.12 "M-2.58 -3.27L7.66 -3.27"
 * field reference at 0, -2 size 1 rotate 90 layer fabrication
 * </pre>
 */
public class FootprintLinesParser extends GrammarLinesParser {

    public static final LineGrammar PAD = LineGrammar.line("pad")
            .slot("number", TEXT)
            .termName("number")
            .slot("type", WORD, BoardVocabulary.PAD_TYPES.toArray(String[]::new))
            .slot("shape", WORD, BoardVocabulary.PAD_SHAPES.toArray(String[]::new))
            .required("at", POINT)
            .optional("rotate", QUANTITY)
            .required("size", EXTENT)
            .optional("drill", EXTENT_OR_QUANTITY)
            .optional("roundness", QUANTITY)
            .ending("outline", DocumentValues.PATH)
            .check((pad, grammar) -> pad.ending().isPresent() && !pad.name("shape").orElse("").equals("custom")
                    ? "only a custom pad is drawn by its own outline" : null)
            .shaped("a pad is 'pad \"NUMBER\" TYPE SHAPE at x, y [rotate degrees] size width x height [drill width [x height]] "
                    + "[roundness share] [\"outline\"]'");

    public static final LineGrammar OUTLINE = LineGrammar.line("outline")
            .slot("layer", WORD, BoardVocabulary.OUTLINE_LAYERS.toArray(String[]::new))
            .optional("width", QUANTITY)
            .optional("filled", FLAG)
            .ending("path", DocumentValues.PATH)
            .check((outline, grammar) -> outline.ending().isEmpty() ? grammar.shape() : null)
            .shaped("an outline is 'outline LAYER [width w] [filled] \"path\"'");

    public static final LineGrammar FIELD = LineGrammar.line("field")
            .slot("subject", WORD, BoardVocabulary.FIELD_SUBJECTS.toArray(String[]::new))
            .required("at", POINT)
            .required("size", QUANTITY)
            .optional("rotate", QUANTITY)
            .optional("layer", WORD, BoardVocabulary.FIELD_LAYERS.toArray(String[]::new))
            .shaped("a field is 'field reference|value at x, y size height [rotate degrees] [layer silkscreen|fabrication]'");

    @Override
    protected List<LineGrammar> grammars() {
        return List.of(PAD, OUTLINE, FIELD);
    }
}
