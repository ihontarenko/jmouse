package org.jmouse.cad.mesh;

import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.GrammarLinesParser;
import org.jmouse.el.grammar.LineGrammar;
import org.jmouse.el.grammar.WordNode;
import org.jmouse.mesh.MeshFormats;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.jmouse.el.grammar.Values.QUANTITIES;
import static org.jmouse.el.grammar.Values.QUANTITY;
import static org.jmouse.el.grammar.Values.TEXT;
import static org.jmouse.el.grammar.Values.WORD;

/**
 * A part's 3D model: the files that show its body, each in an open format ({@link MeshFormats}) — the same body in
 * several, as a library ships it: a mesh a viewer reads fast, exact geometry a STEP export keeps —, or its body drawn
 * here from shapes, boxes and cylinders of a material ({@link ModelShapes}), as a table of packages writes it.
 *
 * <pre>
 * file "SOIC-8_3.9x4.9mm_P1.27mm.wrl" format vrml scale 2.54 up z
 * file "SOIC-8_3.9x4.9mm_P1.27mm.step" format step
 *
 * box ceramic size 1.4, 1.25, 0.45 at 0, 0, 0.225
 * cylinder lacquer radius 0.9 length 3.3 at 0, 0, 0.9 rotate 0, 90, 0
 * prism plastic outline -2.4, 0.6, 2.4, 0.6, 1.7, -1.4, -1.7, -1.4 height 4.5 at 0, 0, 1
 * </pre>
 *
 * <p>{@code scale} is the millimetres one unit of the file is and {@code up} the axis it stands up along — said only
 * where the file keeps another convention than its format's standard, which {@link MeshFormats} holds. The bytes are
 * not the document's: they are files filed against the model, by name. Whose they are is the document's {@code meta}.</p>
 */
public class ModelLinesParser extends GrammarLinesParser {

    public static final LineGrammar FILE = LineGrammar.line("file")
            .slot("name", TEXT)
            .required("format", WORD, MeshFormats.standard().ids().toArray(String[]::new))
            .optional("scale", QUANTITY)
            .optional("up", WORD, Arrays.stream(MeshFormats.Axis.values()).map(axis -> axis.name().toLowerCase(Locale.ROOT))
                    .toArray(String[]::new))
            .build();

    /**
     * A box of a model drawn from shapes: its material ({@link ModelMaterials}), its size along X, Y and Z, its centre, and
     * how it is turned (degrees about X, then Y, then Z) — millimetres, Z up, as a model is set on its footprint.
     */
    public static final LineGrammar BOX = LineGrammar.line("box")
            .slot("material", WORD, ModelMaterials.shipped().names().toArray(String[]::new))
            .required("size", QUANTITIES)
            .optional("at", QUANTITIES)
            .optional("rotate", QUANTITIES)
            .check((box, grammar) -> threeEach(box, "size", "at", "rotate"))
            .build();

    /** A cylinder of a model drawn from shapes: along Z before it is turned, centred where it stands. */
    public static final LineGrammar CYLINDER = LineGrammar.line("cylinder")
            .slot("material", WORD, ModelMaterials.shipped().names().toArray(String[]::new))
            .required("radius", QUANTITY)
            .required("length", QUANTITY)
            .optional("at", QUANTITIES)
            .optional("rotate", QUANTITIES)
            .check((cylinder, grammar) -> threeEach(cylinder, "at", "rotate"))
            .build();

    /**
     * A prism of a model drawn from shapes: a convex outline of the XY plane — {@code x, y} pairs, counter-clockwise seen
     * from above — raised along Z by its height from its base, which {@code at} sets.
     */
    public static final LineGrammar PRISM = LineGrammar.line("prism")
            .slot("material", WORD, ModelMaterials.shipped().names().toArray(String[]::new))
            .required("outline", QUANTITIES)
            .required("height", QUANTITY)
            .optional("at", QUANTITIES)
            .optional("rotate", QUANTITIES)
            .check((prism, grammar) -> prism.quantities("outline").size() < 6 || prism.quantities("outline").size() % 2 != 0
                    ? "a prism's outline is three x, y pairs at least" : threeEach(prism, "at", "rotate"))
            .build();

    /**
     * A body turned about Z: its profile, {@code radius, z} pairs from the bottom up, swept from {@code from} to {@code to}
     * degrees counter-clockwise seen from above — a whole turn when neither is written —, then turned and set as a shape
     * is. A capacitor's rolled can, a resistor's bulging ends, a stripe on a sleeve (a part of a turn).
     */
    public static final LineGrammar REVOLVED = LineGrammar.line("revolved")
            .slot("material", WORD, ModelMaterials.shipped().names().toArray(String[]::new))
            .required("profile", QUANTITIES)
            .optional("from", QUANTITY)
            .optional("to", QUANTITY)
            .optional("at", QUANTITIES)
            .optional("rotate", QUANTITIES)
            .check((revolved, grammar) -> revolved.quantities("profile").size() < 4 || revolved.quantities("profile").size() % 2 != 0
                    ? "a turned body's profile is two radius, z pairs at least" : threeEach(revolved, "at", "rotate"))
            .build();

    /**
     * Where a model's body can be marked and what with ({@link ModelMarkings}): {@code bands}, the part's value in colour
     * bands; {@code value}, the part's value printed; {@code name}, the words its component prints on its body; {@code text}, the words given. {@code on round} a cylinder's side about
     * Z, {@code radius} and {@code length}, from {@code from} to {@code to} degrees — the whole round when neither is
     * written —; {@code on flat} a face looking up Z, {@code size} its width and height. A print is made in an {@code ink}, a
     * material whose colour stands out from what it is printed on. Turned and set as a shape is.
     *
     * <pre>
     * marking(bands, on: round, radius: 1.25, length: 3.6, at: [0, 0, 1.2], rotate: [0, 90, 0])
     * marking(value, on: round, radius: 4.05, length: 5, at: [0, 0, 4], from: 120, to: 240, ink: marking)
     * marking(text, on: flat, size: [1.2, 1.2], at: [2.5, 0, 6.01], text: "+", ink: marking)
     * </pre>
     */
    public static final LineGrammar MARKING = LineGrammar.line("marking")
            .slot("shows", WORD, Arrays.stream(ModelMarkings.Shows.values()).map(shows -> shows.name().toLowerCase(Locale.ROOT))
                    .toArray(String[]::new))
            .required("on", WORD, ModelMarkings.Round.WORD, ModelMarkings.Flat.WORD)
            .optional("radius", QUANTITY)
            .optional("length", QUANTITY)
            .optional("from", QUANTITY)
            .optional("to", QUANTITY)
            .optional("size", QUANTITIES)
            .optional("at", QUANTITIES)
            .optional("rotate", QUANTITIES)
            .optional("text", TEXT)
            .optional("ink", WORD, ModelMaterials.shipped().names().toArray(String[]::new))
            .check((marking, grammar) -> markingFault(marking))
            .build();

    @Override
    protected List<LineGrammar> grammars() {
        return List.of(FILE, BOX, CYLINDER, PRISM, REVOLVED, MARKING);
    }

    private static String markingFault(ClauseLineNode marking) {
        boolean round = ModelMarkings.Round.WORD.equals(marking.word("on").orElse(""));
        if (round && !(marking.has("radius") && marking.has("length"))) {
            return "a marking on round is given its radius and length";
        }
        if (!round && (!marking.has("size") || marking.quantities("size").size() != 2)) {
            return "a marking on flat is given its size: width, height";
        }
        boolean text = marking.slot("shows").map(shows -> shows instanceof WordNode word && ModelMarkings.Shows.TEXT.written().equals(word.word())).orElse(false);
        if (text != marking.has("text")) {
            return text ? "a text marking is given its text" : "only a text marking is given a text";
        }
        boolean bands = marking.slot("shows").map(shows -> shows instanceof WordNode word && ModelMarkings.Shows.BANDS.written().equals(word.word())).orElse(false);
        if (bands == marking.has("ink")) {
            return bands ? "colour bands are in the value's colours, not an ink" : "a print is given its ink: a material of the model's";
        }
        return threeEach(marking, "at", "rotate");
    }

    private static String threeEach(ClauseLineNode line, String... clauses) {
        return Arrays.stream(clauses).anyMatch(clause -> line.has(clause) && line.quantities(clause).size() != 3)
                ? "a shape's " + String.join(", ", clauses) + " are three values each: x, y, z" : null;
    }
}
