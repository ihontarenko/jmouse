package org.jmouse.cad.schematic.drawing;

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
 * @param labels   where the part's name and value are printed
 * @param inputs   what its living part is drawn from
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

    /** Where a pin meets the drawing, and the text printed beside it; an empty label prints nothing. */
    public record Pin(String name, double x, double y, String label) {
    }

    /** Where a part's {@code name} or {@code value} is printed, the text centred on the point. */
    public record Label(String subject, double x, double y) {
    }

    /** Where a part's live figure of this name is drawn: a box in the drawing's coordinates. */
    public record Indicator(String name, double left, double top, double right, double bottom) {
    }

    /**
     * What a living drawing is drawn from: {@code input current 0 .. 20 mA}, {@code input screen picture},
     * {@code input marking text}.
     *
     * @param typical the value drawn when nothing binds it; the low end unless written
     * @param range   the range as written, empty for the default 0 … 1 and for a picture or a text
     * @param picture whether it is a picture: a row of bytes, in hexadecimal
     * @param text    the words a text input is drawn with while no part gives its own; null for any other input
     */
    public record Input(String name, double low, double high, double typical, String range, boolean picture, String text) {
    }

    /** How a value moves: not at all, with the inputs it reads, or in time by itself. */
    public enum Motion {
        STILL, LIVE, ANIMATED
    }

    /**
     * A property's value: as written, its trees — one per value, {@code move} has two —, and how it moves. The trees are
     * read from the words: a value an editor sends holds its words alone, and none until the text is read again.
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
     */
    public record Element(String shape, String name, boolean live, String path, Map<String, List<Double>> geometry,
                          Value condition, Map<String, Value> properties, List<String> hidden, List<Element> children) {

        public Element {
            geometry = Collections.unmodifiableMap(new LinkedHashMap<>(geometry));
            properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
            hidden = List.copyOf(hidden);
            children = List.copyOf(children);
        }
    }
}
