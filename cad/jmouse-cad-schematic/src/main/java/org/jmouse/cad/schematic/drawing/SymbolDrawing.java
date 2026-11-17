package org.jmouse.cad.schematic.drawing;

import org.jspecify.annotations.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A drawing — a symbol's, a portrait's, a frame's — as the interface draws it: its extent, its still paths, its pins
 * and labels, the figures it shows, and its living part — the inputs it is drawn from and its elements, every value an
 * expression tree ({@link DrawingExpression}) checked to be marked as it moves ({@link LivingDrawing}). It is data; how
 * it is sent — JSON, for a browser — is the product's.
 *
 * @param width    the drawing's extent across, centred on the origin; 0 when not written
 * @param height   the drawing's extent down, centred on the origin; 0 when not written
 * @param paths    its still path data, drawn as they stand
 * @param upright  still paths that stay upright however the part is turned — a meter's letter
 * @param pins       where each pin meets it, and what is printed beside it
 * @param labels     where the part's name and value are printed
 * @param indicators where the part's live figures are drawn
 * @param inputs     what its living part is drawn from
 * @param elements its living part's elements
 */
public record SymbolDrawing(double width, double height, List<String> paths, List<String> upright, List<Pin> pins,
                            List<Label> labels, List<Indicator> indicators, List<Input> inputs, List<Element> elements) {

    public SymbolDrawing {
        paths = List.copyOf(paths);
        upright = List.copyOf(upright);
        pins = List.copyOf(pins);
        labels = List.copyOf(labels);
        indicators = List.copyOf(indicators);
        inputs = List.copyOf(inputs);
        elements = List.copyOf(elements);
    }

    /**
     * Where a pin meets the drawing, and the text printed beside it; an empty label prints nothing.
     *
     * @param name  the pin's name, as the part names its pins
     * @param x     where it meets the drawing, across from the origin
     * @param y     where it meets the drawing, down from the origin
     * @param label the text printed beside it
     * @param body  where its lead reaches the body, which its label stands inside; null for a pin whose lead is not its own
     */
    public record Pin(String name, double x, double y, String label, @Nullable Body body) {

        /**
         * A point on the body.
         *
         * @param x across from the origin
         * @param y down from the origin
         */
        public record Body(double x, double y) {
        }
    }

    /**
     * Where a part's {@code name} or {@code value} is printed, the text centred on the point.
     *
     * @param subject {@code name} or {@code value}
     * @param x       across from the origin
     * @param y       down from the origin
     */
    public record Label(String subject, double x, double y) {
    }

    /**
     * Where a part's live figure of this name is drawn: a box in the drawing's coordinates.
     *
     * @param name   the figure it shows, by name
     * @param left   the box's left edge
     * @param top    the box's top edge
     * @param right  the box's right edge
     * @param bottom the box's bottom edge
     */
    public record Indicator(String name, double left, double top, double right, double bottom) {
    }

    /**
     * What a living drawing is drawn from: {@code input current 0 .. 20 mA}, {@code input screen picture},
     * {@code input marking text}.
     *
     * @param name    what it is called, as the drawing's values read it
     * @param low     the low end of its range
     * @param high    the high end of its range
     * @param typical the value drawn when nothing binds it; the low end unless written
     * @param range   the range as written, empty for the default 0 … 1 and for a picture or a text
     * @param picture whether it is a picture: a row of bytes, in hexadecimal
     * @param text    the words a text input is drawn with while no part gives its own; null for any other input
     * @param hold    how long the value takes to settle, in seconds: what a symbol shows of a figure that moves many times a
     *                second — a resistor's power on an alternating current — is read as it settles, not as it flickers. 0 follows it at once.
     */
    public record Input(String name, double low, double high, double typical, String range, boolean picture, @Nullable String text, double hold) {

        /** An input followed at once. */
        public Input(String name, double low, double high, double typical, String range, boolean picture, String text) {
            this(name, low, high, typical, range, picture, text, 0);
        }
    }

    /** How a value moves: not at all, with the inputs it reads, or in time by itself. */
    public enum Motion {
        STILL, LIVE, ANIMATED
    }

    /**
     * A property's value: as written, its trees — one per value, {@code move} has two —, and how it moves. The trees are
     * read from the words: a value an editor sends holds its words alone, and none until the text is read again.
     *
     * @param written     the value as its line writes it
     * @param expressions its trees, which the browser evaluates; none in a value an editor sends
     * @param motion      how it moves
     */
    public record Value(String written, List<DrawingExpression> expressions, Motion motion) {

        public Value {
            expressions = expressions == null ? List.of() : List.copyOf(expressions);
        }
    }

    /**
     * An element of the living part: its shape, its name, whether it is marked {@code live}, a path's data, its geometry
     * by word — {@code at}, {@code size}, {@code radius}, {@code from}, {@code to}, {@code pivot} —, a layer's condition
     * (null for any other element), its properties, what a layer hides, and what a group or a layer holds.
     *
     * @param shape      what it is, by its word: {@code path}, {@code box}, {@code circle}, {@code line}, {@code glyph}, {@code bitmap},
     *                   {@code group}, or {@code when} for a layer
     * @param name       what it is called, for a layer to hide it by; empty for an element of no name
     * @param live       whether it is marked {@code live}: drawn again as its inputs move
     * @param path       a path's data; empty for any other shape
     * @param geometry   its geometry by word — {@code at}, {@code size}, {@code radius}, {@code from}, {@code to}, {@code pivot} —, each a list of numbers
     * @param condition  when a layer is shown; null for any other element
     * @param properties its properties by name — {@code fill}, {@code opacity}, {@code rotate} … —, each a value
     * @param hidden     the elements a layer hides while it is shown, by name
     * @param children   what a group or a layer holds
     */
    public record Element(String shape, String name, boolean live, String path, Map<String, List<Double>> geometry,
                          @Nullable Value condition, Map<String, Value> properties, List<String> hidden, List<Element> children) {

        public Element {
            geometry = Collections.unmodifiableMap(new LinkedHashMap<>(geometry));
            properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
            hidden = List.copyOf(hidden);
            children = List.copyOf(children);
        }
    }
}
