package org.jmouse.cad.mesh;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.el.grammar.ClauseLineNode;
import org.jmouse.el.grammar.WordNode;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 🏷 Where a model's body can be marked, and with what: a resistor's colour bands, a capacitor's printed value, a bridge's
 * signs by its pins. A model is drawn once per package, so what a mark says cannot be in its triangles — every 0207
 * resistor is one body —: the model says only where a mark goes and what kind of thing it shows, and the view that
 * stands a part on a pcb paints it from that part's value. Read from the model document's {@code marking} lines
 * ({@link ModelLinesParser#MARKING}) and carried to a viewer in the body's GLB, as its root's glTF {@code extras}.
 */
public final class ModelMarkings {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** What a marking shows. */
    public enum Shows {
        /** The part's value in colour bands, as IEC 60062 codes a resistance. */
        BANDS,
        /** The part's value, printed. */
        VALUE,
        /** The words the part's component prints on its body — a chip's type, `LM317` —, which its portrait is bound with. */
        NAME,
        /** Words the model itself gives, the same on every part: a sign by a pin. */
        TEXT;

        public String written() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** The surface a marking lies on, in the marking's own frame: about Z, or facing up Z. */
    public sealed interface Surface {

        String written();
    }

    /** Round about Z, centred where the marking stands: a cylinder's side, or the part of it from one angle to another. */
    public record Round(double radius, double length, double from, double to) implements Surface {

        public static final String WORD = "round";

        public String written() {
            return WORD;
        }
    }

    /** Flat, facing up Z, centred where the marking stands. */
    public record Flat(double width, double height) implements Surface {

        public static final String WORD = "flat";

        public String written() {
            return WORD;
        }
    }

    /**
     * One marking: what it shows, on what, where it stands and how it is turned (degrees about X, then Y, then Z), and the ink
     * a print is made in — a material of the model's, chosen against what it is printed on; none for colour bands, whose
     * colours are the value's.
     */
    public record Marking(Shows shows, Surface surface, List<Double> at, List<Double> rotate, Optional<String> text, Optional<String> ink) {
    }

    private static final List<Double> NONE = List.of(0.0, 0.0, 0.0);

    private ModelMarkings() {
    }

    /** The markings a model document gives, in their order; none for a model that gives none. */
    public static List<Marking> of(DocumentNode model) {
        return model.getExpressions().stream()
                .filter(statement -> statement instanceof ClauseLineNode line && line.grammar() == ModelLinesParser.MARKING)
                .map(statement -> markingOf((ClauseLineNode) statement))
                .toList();
    }

    /** The markings as a body's glTF extras — {@code {"markings": [...]}}, an ink as its sRGB colour `#rrggbb` —, or null for none. */
    public static JsonNode extrasOf(List<Marking> markings, ModelMaterials materials) {
        if (markings.isEmpty()) {
            return null;
        }
        ObjectNode extras = JSON.createObjectNode();
        ArrayNode written = extras.putArray("markings");
        for (Marking marking : markings) {
            ObjectNode one = written.addObject().put("shows", marking.shows().written());
            ObjectNode surface = one.putObject("surface").put("kind", marking.surface().written());
            switch (marking.surface()) {
                case Round round -> surface.put("radius", round.radius()).put("length", round.length())
                        .put("from", round.from()).put("to", round.to());
                case Flat flat -> surface.put("width", flat.width()).put("height", flat.height());
            }
            marking.at().forEach(one.putArray("at")::add);
            marking.rotate().forEach(one.putArray("rotate")::add);
            marking.text().ifPresent(text -> one.put("text", text));
            marking.ink().flatMap(materials::colour).ifPresent(colour -> one.put("ink", hex(colour)));
        }
        return extras;
    }

    private static Marking markingOf(ClauseLineNode line) {
        Shows shows = Shows.valueOf(((WordNode) line.slot("shows").orElseThrow()).word().toUpperCase(Locale.ROOT));
        String on = line.word("on").orElseThrow();
        Surface surface = Round.WORD.equals(on)
                ? new Round(line.quantity("radius").orElseThrow(), line.quantity("length").orElseThrow(),
                        line.quantity("from").orElse(0.0), line.quantity("to").orElse(360.0))
                : new Flat(line.quantities("size").get(0), line.quantities("size").get(1));
        Optional<String> text = line.word("text");
        return new Marking(shows, surface, line.has("at") ? line.quantities("at") : NONE,
                line.has("rotate") ? line.quantities("rotate") : NONE, text, line.word("ink"));
    }

    /** An sRGB colour, three shares of 1, as `#rrggbb`. */
    private static String hex(List<Double> colour) {
        StringBuilder written = new StringBuilder("#");
        for (double share : colour) {
            written.append(String.format(Locale.ROOT, "%02x", (int) Math.round(Math.max(0, Math.min(1, share)) * 255)));
        }
        return written.toString();
    }
}
