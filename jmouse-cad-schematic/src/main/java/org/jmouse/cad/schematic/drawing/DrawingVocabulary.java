package org.jmouse.cad.schematic.drawing;

import java.util.Map;
import java.util.Set;

/**
 * The words a living drawing is written with, as data its reader names: the functions the browser evaluates, the
 * mathematics a drawing may call besides, the words a value may be, and each property with how many values it takes.
 */
public final class DrawingVocabulary {

    /** The drawing functions the browser evaluates; {@link #TIMED} among them move on the wall clock. */
    public static final Set<String> FUNCTIONS = Set.of("between", "share", "seen", "curve", "smooth", "blink", "pulse", "wave",
            "bit", "absolute", "minimum", "maximum");

    /** The functions that move in time by themselves. */
    public static final Set<String> TIMED = Set.of("blink", "pulse", "wave");

    /** The jMF functions a drawing may call besides its own. */
    public static final Set<String> MATHEMATICS = Set.of("sin", "cos", "tan", "sqrt", "exp", "ln", "abs", "sign", "floor",
            "ceil", "round", "step", "min", "max");

    /**
     * The words a value may be: what the sheet colours by its own tokens — {@code signal} the drawing's ink, {@code body}
     * a part's body fill as the drawing theme gives it, {@code clear} nothing —, and a portrait's colour code (IEC 60062)
     * and materials.
     */
    public static final Set<String> WORDS = Set.of("clear", "signal", "body", "positive", "negative", "warm", "hot", "burned",
            "glow", "red", "orange", "yellow", "green", "blue", "violet", "white", "yes", "no",
            "black", "brown", "grey", "gold", "silver", "beige", "tan", "copper", "amber");

    /** Each property, and how many values it is written with. */
    public static final Map<String, Set<Integer>> PROPERTIES = Map.ofEntries(
            Map.entry("rotate", Set.of(1)), Map.entry("move", Set.of(2)), Map.entry("scale", Set.of(1, 2)),
            Map.entry("opacity", Set.of(1)), Map.entry("visible", Set.of(1)), Map.entry("fill", Set.of(1)),
            Map.entry("stroke", Set.of(1)), Map.entry("glow", Set.of(1)), Map.entry("level", Set.of(1)),
            Map.entry("dash", Set.of(1)), Map.entry("thickness", Set.of(1)), Map.entry("character", Set.of(1)),
            Map.entry("pixels", Set.of(1)), Map.entry("columns", Set.of(1)), Map.entry("rows", Set.of(1)),
            Map.entry("invert", Set.of(1)), Map.entry("stride", Set.of(1)), Map.entry("first_column", Set.of(1)),
            Map.entry("text", Set.of(1)));

    /** The properties with a meaning of their own: a picture drawn, words printed. */
    public static final String PIXELS = "pixels";
    public static final String TEXT = "text";

    /** The shapes with a property of their own: a bitmap draws a picture, a glyph prints words. */
    public static final String BITMAP = "bitmap";
    public static final String GLYPH = "glyph";

    private DrawingVocabulary() {
    }
}
