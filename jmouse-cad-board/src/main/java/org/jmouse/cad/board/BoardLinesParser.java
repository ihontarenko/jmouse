package org.jmouse.cad.board;

import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.GrammarLinesParser;
import org.jmouse.cad.document.DocumentValues;
import org.jmouse.el.grammar.LineGrammar;

import java.util.List;

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
 * track top width 0.6 rounding 1.5 from 4, 5 to 6.54, 5 to 12, 3
 * via at 20, 9 drill 0.4 diameter 0.8 from top to bottom
 * </pre>
 *
 * Which pads belong together and which footprint a part stands on are its circuit's; the board holds geometry only.
 */
public class BoardLinesParser extends GrammarLinesParser {

    private static LineGrammar.Builder rules() {
        LineGrammar.Builder rules = LineGrammar.line("rules").slot("profile", WORD);
        BoardVocabulary.RULES.stream().sorted().forEach(rule -> rules.optional(rule, QUANTITY));
        return rules;
    }

    /** A {@code sizes} line that says what its word asks: widths for tracks, a drill and a diameter for every via. */
    private static String sizesRefusal(ClauseLineNode line, LineGrammar grammar) {
        boolean tracks = line.name("sized").map(BoardVocabulary.TRACKS::equals).orElse(false);
        if (tracks) {
            return line.quantities("widths").isEmpty() || line.has("drill") || line.has("diameter") ? grammar.shape() : null;
        }
        List<Double> drills = line.quantities("drill");
        List<Double> diameters = line.quantities("diameter");
        return line.slot("widths").isPresent() || drills.isEmpty() || drills.size() != diameters.size() ? grammar.shape() : null;
    }

    private static LineGrammar path(String word, String shape) {
        return LineGrammar.line(word).ending("path", DocumentValues.PATH).check((line, grammar) -> line.ending().isEmpty() ? grammar.shape() : null)
                .shaped(shape);
    }

    static final List<LineGrammar> LINES = List.of(
            LineGrammar.line(BoardVocabulary.CIRCUIT).slot("slug", WORD).shaped("a board names its circuit: 'circuit rc-discharging'"),
            rules().shaped("a board names how it is made, then what it changes: 'rules standard [clearance 0.25 …]'"),
            LineGrammar.line("class").slot("name", WORD).required("width", QUANTITY).required("clearance", QUANTITY)
                    .optional("drill", QUANTITY).optional("diameter", QUANTITY).required("nets", NAMES)
                    .shaped("a net class is 'class NAME width w clearance c [drill d diameter d] nets a, b'"),
            LineGrammar.line("pair").slot("name", WORD).required("width", QUANTITY).required("gap", QUANTITY)
                    .optional("skew", QUANTITY).required("nets", NAMES)
                    .shaped("a differential pair is 'pair NAME width w gap g [skew s] nets positive, negative'"),
            LineGrammar.line("sizes").slot("sized", WORD, BoardVocabulary.SIZED.toArray(String[]::new))
                    .optionalSlot("widths", QUANTITIES).optional("drill", QUANTITIES).optional("diameter", QUANTITIES)
                    .check(BoardLinesParser::sizesRefusal)
                    .shaped("sizes are 'sizes tracks w, w …' or 'sizes vias drill d, d … diameter D, D …', each via's drill and diameter in the same place"),
            LineGrammar.line("teardrops").slot("target", WORD, BoardVocabulary.TEARDROP_TARGETS.toArray(String[]::new))
                    .required("length", QUANTITY).required("width", QUANTITY).optional("curved", FLAG)
                    .shaped("teardrops are 'teardrops round|surface|tracks length l width w [curved]', each a share"),
            LineGrammar.line("layers").slot("count", QUANTITY).shaped("a board says how many copper layers it has: 'layers 2'"),
            path("edge", "a board's edge is SVG path data in millimetres: 'edge \"M0 0H40V20H0Z\"'"),
            LineGrammar.line("place").slot("reference", WORD).required("at", POINT).optional("rotate", QUANTITY)
                    .optional("side", WORD, BoardVocabulary.SIDES.toArray(String[]::new))
                    .nested("reference", LineGrammar.line("reference").required("at", POINT).optional("rotate", QUANTITY)
                            .shaped("a reference stands 'reference at x, y [rotate degrees]'"))
                    .shaped("a part stands as 'place R1 at x, y [rotate degrees] [side top|bottom] [reference at x, y [rotate degrees]]'"),
            LineGrammar.line("track").slot("layer", WORD).required("width", QUANTITY).optional("rounding", QUANTITY)
                    .required("from", POINT).repeated("to", POINT)
                    .check((track, grammar) -> track.values("to").isEmpty() ? grammar.shape() : null)
                    .shaped("a track is 'track LAYER width w [rounding r] from x, y to x, y [to x, y …]'"),
            LineGrammar.line("arc").slot("layer", WORD).required("width", QUANTITY).required("from", POINT)
                    .required("through", POINT).required("to", POINT)
                    .shaped("an arc is 'arc LAYER width w from x, y through x, y to x, y'"),
            LineGrammar.line("via").required("at", POINT).required("drill", QUANTITY).required("diameter", QUANTITY)
                    .optional("from", WORD).optional("to", WORD)
                    .shaped("a via is 'via at x, y drill d diameter d [from LAYER to LAYER]'"),
            LineGrammar.line("zone").slot("layer", WORD).required("net", WORD).optional("clearance", QUANTITY)
                    .optional("priority", QUANTITY).ending("path", DocumentValues.PATH)
                    .check((zone, grammar) -> zone.ending().isEmpty() ? grammar.shape() : null)
                    .shaped("a zone is 'zone LAYER net NAME [clearance c] [priority n] \"path\"'"),
            LineGrammar.line("keepout").slot("layers", NAMES).ending("path", DocumentValues.PATH)
                    .check((keepout, grammar) -> keepout.ending().isEmpty() ? grammar.shape() : null)
                    .shaped("a keepout is 'keepout LAYER[, LAYER …] \"path\"'"),
            LineGrammar.line("text").slot("layer", WORD).required("size", QUANTITY).required("at", POINT)
                    .optional("rotate", QUANTITY).ending("words")
                    .check((text, grammar) -> text.ending().isEmpty() ? grammar.shape() : null)
                    .shaped("a text is 'text top-silkscreen|bottom-silkscreen size h at x, y [rotate degrees] \"words\"'"),
            LineGrammar.line("hole").required("at", POINT).required("drill", QUANTITY).optional("ring", QUANTITY)
                    .shaped("a hole is 'hole at x, y drill d [ring r]', plated when it has a ring"),
            LineGrammar.line("dimension").required("from", POINT).required("to", POINT).optional("offset", QUANTITY)
                    .shaped("a dimension is 'dimension from x, y to x, y [offset d]'"));

    /** The grammar of a board's line by its word: what a program makes the line with. */
    public static LineGrammar grammar(String word) {
        return LINES.stream().filter(line -> line.word().equals(word)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("a board has no '" + word + "' line"));
    }

    @Override
    protected List<LineGrammar> grammars() {
        return LINES;
    }
}
