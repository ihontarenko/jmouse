package org.jmouse.cad.pcb;

import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.GrammarLinesParser;
import org.jmouse.cad.document.DocumentValues;
import org.jmouse.el.grammar.LineGrammar;

import java.util.List;

import static org.jmouse.el.grammar.Values.CONNECTIONS;
import static org.jmouse.el.grammar.Values.FLAG;
import static org.jmouse.el.grammar.Values.NAMES;
import static org.jmouse.el.grammar.Values.POINT;
import static org.jmouse.el.grammar.Values.QUANTITY;
import static org.jmouse.el.grammar.Values.WORD;
import static org.jmouse.el.grammar.Values.QUANTITIES;

/**
 * A printed circuit board, in millimetres: the circuit it lays out, how it is made, its layers and edge, where each part
 * stands, and the copper between them.
 *
 * <pre>
 * circuit rc-discharging
 * rules standard clearance 0.25
 * class power width 1 clearance 0.4 drill 0.6 diameter 1.2 nets vcc, 0
 * layers 2
 * edge "M1 0H43A1 1 0 0 1 44 1V17…Z"
 * place C1 at 20, 3 rotate 90 side bottom reference at 2.6, 0 rotate 90
 * place power_R3 shown R3 at 12, 4
 * place H1 footprint mounting-hole-3-2 nets (GND = 1) at 3, 3
 * track top width 0.6 rounding 1.5 from 4, 5 to 6.54, 5 to 12, 3
 * via at 20, 9 drill 0.4 diameter 0.8 from top to bottom
 * </pre>
 *
 * Which pads belong together and which footprint a part stands on are its circuit's — but for what only the pcb has, a
 * mounting hole, which names its own —; the pcb holds geometry only.
 */
public class PcbLinesParser extends GrammarLinesParser {

    private static LineGrammar.Builder rules() {
        LineGrammar.Builder rules = LineGrammar.line(PcbWords.RULES).slot(PcbWords.PROFILE, WORD);
        PcbVocabulary.RULES.stream().sorted().forEach(rule -> rules.optional(rule, QUANTITY));
        return rules;
    }

    /** A {@code sizes} line that says what its word asks: widths for tracks, a drill and a diameter for every via. */
    private static String sizesRefusal(ClauseLineNode line, LineGrammar grammar) {
        boolean tracks = line.name(PcbWords.SIZED).map(PcbVocabulary.TRACKS::equals).orElse(false);
        if (tracks) {
            return line.quantities(PcbWords.WIDTHS).isEmpty() || line.has(PcbWords.DRILL) || line.has(PcbWords.DIAMETER) ? grammar.shape() : null;
        }
        List<Double> drills = line.quantities(PcbWords.DRILL);
        List<Double> diameters = line.quantities(PcbWords.DIAMETER);
        return line.slot(PcbWords.WIDTHS).isPresent() || drills.isEmpty() || drills.size() != diameters.size() ? grammar.shape() : null;
    }

    private static LineGrammar path(String word) {
        return LineGrammar.line(word).ending(PcbWords.PATH, DocumentValues.PATH).check((line, grammar) -> line.ending().isEmpty() ? grammar.shape() : null)
                .build();
    }

    static final List<LineGrammar> LINES = List.of(
            LineGrammar.line(PcbVocabulary.CIRCUIT).slot(PcbWords.SLUG, WORD).build(),
            rules().build(),
            LineGrammar.line(PcbWords.CLASS).slot(PcbWords.NAME, WORD).required(PcbWords.WIDTH, QUANTITY).required(PcbWords.CLEARANCE, QUANTITY)
                    .optional(PcbWords.DRILL, QUANTITY).optional(PcbWords.DIAMETER, QUANTITY).required(PcbWords.NETS, NAMES)
                    .build(),
            LineGrammar.line(PcbWords.PAIR).slot(PcbWords.NAME, WORD).required(PcbWords.WIDTH, QUANTITY).required(PcbWords.GAP, QUANTITY)
                    .optional(PcbWords.SKEW, QUANTITY).required(PcbWords.NETS, NAMES)
                    .build(),
            LineGrammar.line(PcbWords.SIZES).slot(PcbWords.SIZED, WORD, PcbVocabulary.SIZED.toArray(String[]::new))
                    .optionalSlot("widths", QUANTITIES).optional(PcbWords.DRILL, QUANTITIES).optional(PcbWords.DIAMETER, QUANTITIES)
                    .check(PcbLinesParser::sizesRefusal)
                    .build(),
            LineGrammar.line(PcbWords.TEARDROPS).slot(PcbWords.TARGET, WORD, PcbVocabulary.TEARDROP_TARGETS.toArray(String[]::new))
                    .required(PcbWords.LENGTH, QUANTITY).required(PcbWords.WIDTH, QUANTITY).optional(PcbWords.CURVED, FLAG)
                    .build(),
            LineGrammar.line(PcbWords.LAYERS).slot(PcbWords.COUNT, QUANTITY).build(),
            path("edge"),
            LineGrammar.line(PcbWords.PLACE).slot(PcbWords.REFERENCE, WORD).optional(PcbWords.SHOWN, WORD).optional(PcbWords.FOOTPRINT, WORD).optional(PcbWords.NETS, CONNECTIONS)
                    .required(PcbWords.AT, POINT).optional(PcbWords.ROTATE, QUANTITY)
                    .optional(PcbWords.SIDE, WORD, PcbVocabulary.SIDES.toArray(String[]::new))
                    .optional(PcbWords.GROUP, WORD)
                    .optional(PcbWords.ROLE, WORD, PcbVocabulary.PLACEMENT_ROLES.toArray(String[]::new))
                    .nested("reference", LineGrammar.line(PcbWords.REFERENCE).required(PcbWords.AT, POINT).optional(PcbWords.ROTATE, QUANTITY)
                            .optional(PcbWords.LAYER, WORD, PcbVocabulary.FIELD_LAYERS.toArray(String[]::new))
                            .build())
                    .build(),
            LineGrammar.line(PcbWords.TRACK).slot(PcbWords.LAYER, WORD).required(PcbWords.WIDTH, QUANTITY).optional(PcbWords.ROUNDING, QUANTITY)
                    .required(PcbWords.FROM, POINT).repeated(PcbWords.TO, POINT).optional(PcbWords.GROUP, WORD).optional(PcbWords.FINISH, WORD, PcbVocabulary.COPPER_FINISHES.toArray(String[]::new))
                    .check((track, grammar) -> track.values("to").isEmpty() ? grammar.shape() : null)
                    .build(),
            LineGrammar.line(PcbWords.ARC).slot(PcbWords.LAYER, WORD).required(PcbWords.WIDTH, QUANTITY).required(PcbWords.FROM, POINT)
                    .required(PcbWords.THROUGH, POINT).required(PcbWords.TO, POINT).optional(PcbWords.GROUP, WORD).optional(PcbWords.FINISH, WORD, PcbVocabulary.COPPER_FINISHES.toArray(String[]::new))
                    .build(),
            LineGrammar.line(PcbWords.VIA).required(PcbWords.AT, POINT).required(PcbWords.DRILL, QUANTITY).required(PcbWords.DIAMETER, QUANTITY)
                    .optional(PcbWords.FROM, WORD).optional(PcbWords.TO, WORD)
                    .optional(PcbWords.OPEN, WORD, PcbVocabulary.VIA_OPENINGS.toArray(String[]::new)).optional(PcbWords.GROUP, WORD)
                    .build(),
            LineGrammar.line(PcbWords.ZONE).slot(PcbWords.LAYER, WORD).required(PcbWords.NET, WORD).optional(PcbWords.CLEARANCE, QUANTITY)
                    .optional(PcbWords.PRIORITY, QUANTITY).optional(PcbWords.FINISH, WORD, PcbVocabulary.COPPER_FINISHES.toArray(String[]::new)).ending(PcbWords.PATH, DocumentValues.PATH)
                    .check((zone, grammar) -> zone.ending().isEmpty() ? grammar.shape() : null)
                    .build(),
            LineGrammar.line(PcbWords.KEEPOUT).slot(PcbWords.LAYERS, NAMES).ending(PcbWords.PATH, DocumentValues.PATH)
                    .check((keepout, grammar) -> keepout.ending().isEmpty() ? grammar.shape() : null)
                    .build(),
            LineGrammar.line(PcbWords.TEXT).slot(PcbWords.LAYER, WORD).required(PcbWords.SIZE, QUANTITY).required(PcbWords.AT, POINT)
                    .optional(PcbWords.ROTATE, QUANTITY).optional(PcbWords.ANCHOR, WORD, PcbVocabulary.TEXT_ANCHORS.toArray(String[]::new)).ending(PcbWords.WORDS)
                    .check((text, grammar) -> text.ending().isEmpty() ? grammar.shape() : null)
                    .build(),
            LineGrammar.line(PcbWords.SHAPE).slot(PcbWords.LAYER, WORD).optional(PcbWords.WIDTH, QUANTITY).optional(PcbWords.FILLED, FLAG)
                    .optional(PcbWords.NET, WORD).ending(PcbWords.PATH, DocumentValues.PATH)
                    .check((shape, grammar) -> shape.ending().isEmpty() ? grammar.shape() : null)
                    .build(),
            LineGrammar.line(PcbWords.OUTPUT).slot(PcbWords.NAME, WORD).required(PcbWords.FORMATS, NAMES).ending(PcbWords.TITLE)
                    .build(),
            LineGrammar.line(PcbWords.HOLE).required(PcbWords.AT, POINT).required(PcbWords.DRILL, QUANTITY).optional(PcbWords.RING, QUANTITY)
                    .build(),
            LineGrammar.line(PcbWords.DIMENSION).required(PcbWords.FROM, POINT).required(PcbWords.TO, POINT).optional(PcbWords.OFFSET, QUANTITY)
                    .build());

    /** The grammar of a pcb's line by its word: what a program makes the line with. */
    public static LineGrammar grammar(String word) {
        return LINES.stream().filter(line -> line.word().equals(word)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("a pcb has no '" + word + "' line"));
    }

    @Override
    protected List<LineGrammar> grammars() {
        return LINES;
    }
}
