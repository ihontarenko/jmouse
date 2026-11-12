package org.jmouse.cad.pcb;

import org.jmouse.el.grammar.GrammarLinesParser;
import org.jmouse.cad.document.DocumentValues;
import org.jmouse.el.grammar.LineGrammar;

import java.util.List;

import static org.jmouse.el.grammar.Values.EXTENT;
import static org.jmouse.el.grammar.Values.EXTENT_OR_QUANTITY;
import static org.jmouse.el.grammar.Values.FLAG;
import static org.jmouse.el.grammar.Values.POINT;
import static org.jmouse.el.grammar.Values.QUANTITIES;
import static org.jmouse.el.grammar.Values.QUANTITY;
import static org.jmouse.el.grammar.Values.TEXT;
import static org.jmouse.el.grammar.Values.WORD;

/**
 * How a part sits on a pcb, in millimetres: its pads, its outlines, where its reference and value are printed.
 *
 * <pre>
 * pad "1" through-hole rectangle at 0, 0 size 1.905 x 2 drill 1.1
 * pad "2" surface rounded-rectangle at 2.54, 0 rotate 90 size 1 x 1.5 roundness 0.25
 * outline silkscreen width 0.12 "M-2.58 -3.27L7.66 -3.27"
 * field reference at 0, -2 size 1 rotate 90 layer fabrication
 * body height 1.75
 * model "kicad-package-so-soic-8-3-9x4-9mm-p1-27mm-model" rotate 0, 0, -90
 * </pre>
 */
public class FootprintLinesParser extends GrammarLinesParser {

    public static final LineGrammar PAD = LineGrammar.line(PcbWords.PAD)
            .slot(PcbWords.NUMBER, TEXT)
            .termName("number")
            .slot(PcbWords.TYPE, WORD, PcbVocabulary.PAD_TYPES.toArray(String[]::new))
            .slot(PcbWords.SHAPE, WORD, PcbVocabulary.PAD_SHAPES.toArray(String[]::new))
            .required(PcbWords.AT, POINT)
            .optional(PcbWords.ROTATE, QUANTITY)
            .required(PcbWords.SIZE, EXTENT)
            .optional(PcbWords.DRILL, EXTENT_OR_QUANTITY)
            .optional(PcbWords.ROUNDNESS, QUANTITY)
            .optional(PcbWords.FACE, WORD, PcbVocabulary.PAD_FACES.toArray(String[]::new))
            .optional(PcbWords.OPENS, WORD, PcbVocabulary.PAD_OPENINGS.toArray(String[]::new))
            .optional(PcbWords.MASK_MARGIN, QUANTITY)
            .optional(PcbWords.PASTE_MARGIN, QUANTITY)
            .ending(PcbWords.OUTLINE, DocumentValues.PATH)
            .check((pad, grammar) -> pad.ending().isPresent() && !pad.name(PcbWords.SHAPE).orElse("").equals("custom")
                    ? "only a custom pad is drawn by its own outline" : null)
            .build();

    public static final LineGrammar OUTLINE = LineGrammar.line(PcbWords.OUTLINE)
            .slot(PcbWords.LAYER, WORD, PcbVocabulary.OUTLINE_LAYERS.toArray(String[]::new))
            .optional(PcbWords.WIDTH, QUANTITY)
            .optional(PcbWords.FILLED, FLAG)
            .ending(PcbWords.PATH, DocumentValues.PATH)
            .check((outline, grammar) -> outline.ending().isEmpty() ? grammar.shape() : null)
            .build();

    public static final LineGrammar FIELD = LineGrammar.line(PcbWords.FIELD)
            .slot(PcbWords.SUBJECT, WORD, PcbVocabulary.FIELD_SUBJECTS.toArray(String[]::new))
            .required(PcbWords.AT, POINT)
            .required(PcbWords.SIZE, QUANTITY)
            .optional(PcbWords.ROTATE, QUANTITY)
            .optional(PcbWords.LAYER, WORD, PcbVocabulary.FIELD_LAYERS.toArray(String[]::new))
            .optional(PcbWords.ANCHOR, WORD, PcbVocabulary.TEXT_ANCHORS.toArray(String[]::new))
            .build();

    /** The package's body over the footprint: how high it stands; its outline is the fabrication layer's. */
    public static final LineGrammar BODY = LineGrammar.line(PcbWords.BODY)
            .required(PcbWords.HEIGHT, QUANTITY)
            .build();

    /**
     * A 3D model that shows the package's body: the model document it names, and how the model is set onto the footprint
     * — as a model is placed, X to the right, Y up the drawing (against its pads' Y), Z out of the pcb: {@code offset} in
     * millimetres, {@code rotate} in degrees about each axis, right-handed, {@code scale} each axis' factor. Where the
     * model comes from is the model document's; a footprint names none of it.
     */
    public static final LineGrammar MODEL = LineGrammar.line(PcbWords.MODEL)
            .slot(PcbWords.REFERENCE, TEXT)
            .optional(PcbWords.OFFSET, QUANTITIES)
            .optional(PcbWords.ROTATE, QUANTITIES)
            .optional(PcbWords.SCALE, QUANTITIES)
            .optional(PcbWords.HIDDEN, FLAG)
            .check((model, grammar) -> List.of("offset", "rotate", "scale").stream()
                    .anyMatch(clause -> model.has(clause) && model.quantities(clause).size() != 3)
                    ? "a model's offset, rotate and scale are three values each: x, y, z" : null)
            .build();

    @Override
    protected List<LineGrammar> grammars() {
        return List.of(PAD, OUTLINE, FIELD, BODY, MODEL);
    }
}
