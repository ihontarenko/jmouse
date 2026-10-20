package org.jmouse.parallax.script.engine;

import org.jmouse.parallax.script.Block.SymbolMotion;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * A symbol's drawing — {@code {"version": 1, "width": 60, "height": 40, "paths": ["M…"], "pins": [{"name": "a",
 * "x": -30, "y": 0}], "labels": [{"subject": "name", "x": 0, "y": -26}]}}, the JSON
 * {@link org.jmouse.parallax.object.ObjectDefinition.Symbol} carries — and the same drawing as jMX lines:
 * {@code size 60 x 40}, {@code path "M…"}, {@code pin a at -30, 0}, {@code label name at 0, -26}.
 *
 * <p>The drawing is turned into a tree of ordered maps and lists here and into text by {@link JsonText}. The tree is
 * canonical — keys in the order above, coordinates as decimals, a size only when one is written, an empty list left
 * out — so two equal drawings are equal texts.</p>
 *
 * @param width    the drawing's extent across, centred on the origin; 0 when not written
 * @param height   the drawing's extent down, centred on the origin; 0 when not written
 * @param labels   where the part's name and value are printed
 * @param upright  paths that stay upright however the part is turned — a meter's letter —, {@code "upright": […]} in
 *                 the JSON
 * @param inputs   what a living drawing is drawn from ({@link LivingDrawing})
 * @param elements a living drawing's elements
 */
record SymbolDrawing(double width, double height, List<String> paths, List<Pin> pins, List<Label> labels,
                     List<Indicator> indicators, List<String> upright, List<Input> inputs, List<Element> elements) {

    /** The only drawing version there is. */
    static final int VERSION = 1;

    /** The words an element is placed by, in the order they are written. */
    static final List<String> GEOMETRY = List.of("at", "size", "radius", "from", "to", "pivot");

    /** Where a pin meets the drawing, and the text printed beside it; an empty label prints nothing. */
    record Pin(String name, double x, double y, String label) {
    }

    /** Where a part's {@code name} or {@code value} is printed, the text centred on the point. */
    record Label(String subject, double x, double y) {
    }

    /** Where a part's live figure of this name is drawn: a box in the symbol's coordinates. */
    record Indicator(String name, double left, double top, double right, double bottom) {
    }

    /**
     * What a living drawing is drawn from: {@code input current 0 .. 20 mA}, or {@code input screen picture}.
     *
     * @param range   the range as written, empty for the default 0 … 1 and for a picture
     * @param picture whether it is a picture: a row of bytes, in hexadecimal
     */
    record Input(String name, double low, double high, String range, boolean picture) {
    }

    /**
     * A property's value: as written, as expression trees ({@link LivingDrawing}) — one per value, {@code move} has two
     * —, and how it moves: {@code still}, {@code live} or {@code animated} in the JSON.
     */
    record Value(String written, List<Object> expressions, SymbolMotion motion) {
        Value {
            expressions = List.copyOf(expressions);
        }
    }

    /**
     * An element of a living drawing ({@link org.jmouse.parallax.script.Block.SymbolElement}): its shape, name, mark,
     * path data, geometry by word, a {@code when}'s condition (null otherwise), properties, what a layer hides and what a
     * group or a layer holds.
     */
    record Element(String shape, String name, boolean live, String path, Map<String, List<Double>> geometry,
                   Value condition, Map<String, Value> properties, List<String> hidden, List<Element> children) {
        Element {
            geometry = Collections.unmodifiableMap(new LinkedHashMap<>(geometry));
            properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
            hidden = List.copyOf(hidden);
            children = List.copyOf(children);
        }
    }

    SymbolDrawing {
        paths = List.copyOf(paths);
        pins = List.copyOf(pins);
        labels = List.copyOf(labels);
        indicators = indicators == null ? List.of() : List.copyOf(indicators);
        upright = upright == null ? List.of() : List.copyOf(upright);
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
        elements = elements == null ? List.of() : List.copyOf(elements);
    }

    SymbolDrawing(double width, double height, List<String> paths, List<Pin> pins, List<Label> labels) {
        this(width, height, paths, pins, labels, List.of());
    }

    SymbolDrawing(double width, double height, List<String> paths, List<Pin> pins, List<Label> labels, List<Indicator> indicators) {
        this(width, height, paths, pins, labels, indicators, List.of());
    }

    SymbolDrawing(double width, double height, List<String> paths, List<Pin> pins, List<Label> labels,
                  List<Indicator> indicators, List<String> upright) {
        this(width, height, paths, pins, labels, indicators, upright, List.of(), List.of());
    }

    // ─── reading ────────────────────────────────────────────────────────────────────────────────────

    /** The drawing a symbol definition holds. */
    static SymbolDrawing fromJson(String json) {
        Fields drawing = Fields.of(JsonText.read(json), "A symbol drawing");
        if (drawing.number("version", -1) != VERSION) {
            throw new IllegalArgumentException("A symbol drawing is version " + VERSION + ".");
        }
        return new SymbolDrawing(drawing.number("width", 0), drawing.number("height", 0), drawing.texts("paths"),
                drawing.objects("pins", "A symbol pin", pin -> new Pin(pin.text("name"), pin.number("x"), pin.number("y"),
                        pin.text("label", ""))),
                drawing.objects("labels", "A symbol label", label -> new Label(label.text("subject"), label.number("x"),
                        label.number("y"))),
                drawing.objects("indicators", "A symbol indicator", indicator -> new Indicator(indicator.text("name"),
                        indicator.number("left"), indicator.number("top"), indicator.number("right"), indicator.number("bottom"))),
                drawing.texts("upright"),
                drawing.objects("inputs", "A symbol input", input -> new Input(input.text("name"), input.number("low"),
                        input.number("high"), input.text("range", ""), input.truth("picture"))),
                drawing.objects("elements", "A symbol element", SymbolDrawing::element));
    }

    private static Element element(Fields element) {
        Map<String, List<Double>> geometry = new LinkedHashMap<>();
        GEOMETRY.stream().filter(element::has).forEach(word -> geometry.put(word, element.numbers(word)));
        Map<String, Value> properties = new LinkedHashMap<>();
        element.fields("properties").entries().forEach((name, value) ->
                properties.put(String.valueOf(name), value(Fields.of(value, "A symbol value"))));
        return new Element(element.text("shape"), element.text("name", ""), element.truth("live"), element.text("path", ""),
                geometry, element.has("when") ? value(element.fields("when")) : null, properties, element.texts("hide"),
                element.objects("children", "A symbol element", SymbolDrawing::element));
    }

    private static Value value(Fields value) {
        return new Value(value.text("written"), value.list("values"), SymbolMotion.valueOf(value.text("motion").toUpperCase(Locale.ROOT)));
    }

    /** One JSON object's fields, read by kind; a refusal names what the object is. */
    private record Fields(Map<?, ?> entries, String what) {

        static Fields of(Object value, String what) {
            if (!(value instanceof Map<?, ?> entries)) {
                throw new IllegalArgumentException(what + " is a JSON object.");
            }
            return new Fields(entries, what);
        }

        boolean has(String key) {
            return entries.get(key) != null;
        }

        double number(String key) {
            if (!(entries.get(key) instanceof Double number)) {
                throw new IllegalArgumentException(what + " has a number \"" + key + "\".");
            }
            return number;
        }

        double number(String key, double fallback) {
            return has(key) ? number(key) : fallback;
        }

        String text(String key) {
            if (!(entries.get(key) instanceof String text)) {
                throw new IllegalArgumentException(what + " has a text \"" + key + "\".");
            }
            return text;
        }

        String text(String key, String fallback) {
            return has(key) ? text(key) : fallback;
        }

        boolean truth(String key) {
            return Boolean.TRUE.equals(entries.get(key));
        }

        List<Object> list(String key) {
            if (!has(key)) {
                return List.of();
            }
            if (!(entries.get(key) instanceof List<?> items)) {
                throw new IllegalArgumentException(what + " has an array \"" + key + "\".");
            }
            return List.copyOf(items);
        }

        List<String> texts(String key) {
            return list(key).stream().map(String::valueOf).toList();
        }

        List<Double> numbers(String key) {
            return list(key).stream().map(item -> {
                if (!(item instanceof Double number)) {
                    throw new IllegalArgumentException(what + " has numbers in \"" + key + "\".");
                }
                return number;
            }).toList();
        }

        Fields fields(String key) {
            return has(key) ? Fields.of(entries.get(key), what + "'s \"" + key + "\"") : new Fields(Map.of(), what);
        }

        <T> List<T> objects(String key, String itemWhat, Function<Fields, T> reader) {
            return list(key).stream().map(item -> reader.apply(Fields.of(item, itemWhat))).toList();
        }
    }

    // ─── writing ────────────────────────────────────────────────────────────────────────────────────

    /** The canonical JSON of this drawing. */
    String toJson() {
        JsonObject drawing = new JsonObject().put("version", VERSION);
        if (width > 0 || height > 0) {
            drawing.put("width", width).put("height", height);
        }
        return JsonText.write(drawing.put("paths", paths)
                .putUnlessEmpty("upright", upright)
                .putUnlessEmpty("pins", pins.stream().map(SymbolDrawing::pinTree).toList())
                .putUnlessEmpty("labels", labels.stream().map(label -> new JsonObject().put("subject", label.subject())
                        .put("x", label.x()).put("y", label.y()).entries()).toList())
                .putUnlessEmpty("indicators", indicators.stream().map(indicator -> new JsonObject().put("name", indicator.name())
                        .put("left", indicator.left()).put("top", indicator.top()).put("right", indicator.right())
                        .put("bottom", indicator.bottom()).entries()).toList())
                .putUnlessEmpty("inputs", inputs.stream().map(SymbolDrawing::inputTree).toList())
                .putUnlessEmpty("elements", elements.stream().map(SymbolDrawing::elementTree).toList())
                .entries());
    }

    private static Map<String, Object> pinTree(Pin pin) {
        return new JsonObject().put("name", pin.name()).put("x", pin.x()).put("y", pin.y())
                .putUnlessEmpty("label", pin.label()).entries();
    }

    private static Map<String, Object> inputTree(Input input) {
        JsonObject tree = new JsonObject().put("name", input.name()).put("low", input.low()).put("high", input.high())
                .putUnlessEmpty("range", input.range());
        // Written only for a picture: every number input's tree stays as it was.
        if (input.picture()) {
            tree.put("picture", true);
        }
        return tree.entries();
    }

    private static Map<String, Object> elementTree(Element element) {
        JsonObject tree = new JsonObject().put("shape", element.shape()).putUnlessEmpty("name", element.name());
        if (element.live()) {
            tree.put("live", true);
        }
        tree.putUnlessEmpty("path", element.path());
        element.geometry().forEach(tree::put);
        if (element.condition() != null) {
            tree.put("when", valueTree(element.condition()));
        }
        if (!element.properties().isEmpty()) {
            JsonObject properties = new JsonObject();
            element.properties().forEach((name, value) -> properties.put(name, valueTree(value)));
            tree.put("properties", properties.entries());
        }
        return tree.putUnlessEmpty("hide", element.hidden())
                .putUnlessEmpty("children", element.children().stream().map(SymbolDrawing::elementTree).toList())
                .entries();
    }

    private static Map<String, Object> valueTree(Value value) {
        return new JsonObject().put("written", value.written()).put("values", value.expressions())
                .put("motion", value.motion().name().toLowerCase(Locale.ROOT)).entries();
    }

    /** A JSON object being built: its entries in the order they are put, an empty list or text left out on request. */
    private static final class JsonObject {

        private final Map<String, Object> entries = new LinkedHashMap<>();

        JsonObject put(String key, Object value) {
            entries.put(key, value);
            return this;
        }

        JsonObject putUnlessEmpty(String key, List<?> items) {
            return items.isEmpty() ? this : put(key, items);
        }

        JsonObject putUnlessEmpty(String key, String text) {
            return text.isEmpty() ? this : put(key, text);
        }

        Map<String, Object> entries() {
            return entries;
        }
    }
}
