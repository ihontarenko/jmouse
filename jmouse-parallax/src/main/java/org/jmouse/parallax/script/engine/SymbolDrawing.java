package org.jmouse.parallax.script.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A symbol's drawing — {@code {"version": 1, "width": 60, "height": 40, "paths": ["M…"], "pins": [{"name": "a",
 * "x": -30, "y": 0}], "labels": [{"subject": "name", "x": 0, "y": -26}]}}, the JSON
 * {@link org.jmouse.parallax.object.ObjectDefinition.Symbol} carries — and the same drawing as jMX lines:
 * {@code size 60 x 40}, {@code path "M…"}, {@code pin a at -30, 0}, {@code label name at 0, -26}.
 *
 * <p>The JSON is read by a reader of its own, small because the document is: this module carries no JSON library,
 * and the drawing is the one JSON a definition holds. Writing gives the canonical form — keys in the order above,
 * coordinates as decimals, a size only when one is written — so two equal drawings are equal texts.</p>
 *
 * @param width  the drawing's extent across, centred on the origin; 0 when not written
 * @param height the drawing's extent down, centred on the origin; 0 when not written
 * @param labels where the part's name and value are printed
 * @param upright paths that stay upright however the part is turned — a meter's letter —, {@code "upright": […]} in the
 *                JSON, written only when there are any
 */
record SymbolDrawing(double width, double height, List<String> paths, List<Pin> pins, List<Label> labels,
                     List<Indicator> indicators, List<String> upright) {

    /** Where a part's live figure of this name is drawn: a box in the symbol's coordinates. */
    record Indicator(String name, double left, double top, double right, double bottom) {
    }

    SymbolDrawing(double width, double height, List<String> paths, List<Pin> pins, List<Label> labels) {
        this(width, height, paths, pins, labels, List.of());
    }

    SymbolDrawing(double width, double height, List<String> paths, List<Pin> pins, List<Label> labels, List<Indicator> indicators) {
        this(width, height, paths, pins, labels, indicators, List.of());
    }

    /** The only drawing version there is. */
    static final int VERSION = 1;

    SymbolDrawing {
        paths = List.copyOf(paths);
        pins = List.copyOf(pins);
        labels = List.copyOf(labels);
        indicators = indicators == null ? List.of() : List.copyOf(indicators);
        upright = upright == null ? List.of() : List.copyOf(upright);
    }

    /** Where a pin meets the drawing, and the text printed beside it; an empty label prints nothing. */
    record Pin(String name, double x, double y, String label) {
    }

    /** Where a part's {@code name} or {@code value} is printed, the text centred on the point. */
    record Label(String subject, double x, double y) {
    }

    // ─── JSON ───────────────────────────────────────────────────────────────────────────────────────

    /** The drawing a symbol definition holds. */
    static SymbolDrawing fromJson(String json) {
        Object document = new JsonReading(json).document();
        if (!(document instanceof Map<?, ?> drawing)) {
            throw new IllegalArgumentException("A symbol drawing is a JSON object.");
        }
        if (!(drawing.get("version") instanceof Double version) || version != VERSION) {
            throw new IllegalArgumentException("A symbol drawing is version " + VERSION + ".");
        }
        double width = drawing.get("width") instanceof Double written ? written : 0;
        double height = drawing.get("height") instanceof Double written ? written : 0;
        List<String> paths = texts(drawing.get("paths"));
        List<Pin> pins = new ArrayList<>();
        if (drawing.get("pins") instanceof List<?> written) {
            for (Object entry : written) {
                if (!(entry instanceof Map<?, ?> pin) || !(pin.get("name") instanceof String name)
                        || !(pin.get("x") instanceof Double x) || !(pin.get("y") instanceof Double y)) {
                    throw new IllegalArgumentException("A symbol pin is {\"name\": …, \"x\": …, \"y\": …}.");
                }
                pins.add(new Pin(name, x, y, pin.get("label") instanceof String label ? label : ""));
            }
        }
        List<Label> labels = new ArrayList<>();
        if (drawing.get("labels") instanceof List<?> written) {
            for (Object entry : written) {
                if (!(entry instanceof Map<?, ?> label) || !(label.get("subject") instanceof String subject)
                        || !(label.get("x") instanceof Double x) || !(label.get("y") instanceof Double y)) {
                    throw new IllegalArgumentException("A symbol label is {\"subject\": …, \"x\": …, \"y\": …}.");
                }
                labels.add(new Label(subject, x, y));
            }
        }
        List<Indicator> indicators = new ArrayList<>();
        if (drawing.get("indicators") instanceof List<?> written) {
            for (Object entry : written) {
                if (!(entry instanceof Map<?, ?> indicator) || !(indicator.get("name") instanceof String name)
                        || !(indicator.get("left") instanceof Double left) || !(indicator.get("top") instanceof Double top)
                        || !(indicator.get("right") instanceof Double right) || !(indicator.get("bottom") instanceof Double bottom)) {
                    throw new IllegalArgumentException("A symbol indicator is {\"name\": …, \"left\": …, \"top\": …, \"right\": …, \"bottom\": …}.");
                }
                indicators.add(new Indicator(name, left, top, right, bottom));
            }
        }
        return new SymbolDrawing(width, height, paths, pins, labels, indicators, texts(drawing.get("upright")));
    }

    /** A JSON array of path texts; nothing when the key is not written. */
    private static List<String> texts(Object written) {
        List<String> texts = new ArrayList<>();
        if (written instanceof List<?> items) {
            for (Object item : items) {
                texts.add(String.valueOf(item));
            }
        }
        return texts;
    }

    /** The canonical JSON of this drawing. */
    String toJson() {
        StringBuilder json = new StringBuilder("{\"version\":").append(VERSION);
        if (width > 0 || height > 0) {
            json.append(",\"width\":").append(width).append(",\"height\":").append(height);
        }
        json.append(",\"paths\":[").append(String.join(",", paths.stream().map(SymbolDrawing::quoted).toList())).append(']');
        if (!upright.isEmpty()) {
            json.append(",\"upright\":[").append(String.join(",", upright.stream().map(SymbolDrawing::quoted).toList())).append(']');
        }
        if (!pins.isEmpty()) {
            json.append(",\"pins\":[").append(String.join(",", pins.stream().map(SymbolDrawing::pinJson).toList())).append(']');
        }
        if (!labels.isEmpty()) {
            json.append(",\"labels\":[").append(String.join(",", labels.stream().map(label -> "{\"subject\":"
                    + quoted(label.subject()) + ",\"x\":" + label.x() + ",\"y\":" + label.y() + "}").toList())).append(']');
        }
        if (!indicators.isEmpty()) {
            json.append(",\"indicators\":[").append(String.join(",", indicators.stream().map(indicator -> "{\"name\":"
                    + quoted(indicator.name()) + ",\"left\":" + indicator.left() + ",\"top\":" + indicator.top()
                    + ",\"right\":" + indicator.right() + ",\"bottom\":" + indicator.bottom() + "}").toList())).append(']');
        }
        return json.append('}').toString();
    }

    private static String pinJson(Pin pin) {
        String label = pin.label().isEmpty() ? "" : ",\"label\":" + quoted(pin.label());
        return "{\"name\":" + quoted(pin.name()) + ",\"x\":" + pin.x() + ",\"y\":" + pin.y() + label + "}";
    }

    private static String quoted(String text) {
        StringBuilder quoted = new StringBuilder("\"");
        for (char character : text.toCharArray()) {
            switch (character) {
                case '"' -> quoted.append("\\\"");
                case '\\' -> quoted.append("\\\\");
                case '\n' -> quoted.append("\\n");
                case '\r' -> quoted.append("\\r");
                case '\t' -> quoted.append("\\t");
                default -> {
                    if (character < 0x20) {
                        quoted.append(String.format("\\u%04x", (int) character));
                    } else {
                        quoted.append(character);
                    }
                }
            }
        }
        return quoted.append('"').toString();
    }

    /** One pass over a JSON text: objects, arrays, texts, numbers (as {@code Double}), truth values and null. */
    private static final class JsonReading {

        private final String json;
        private int position;

        JsonReading(String json) {
            this.json = json;
        }

        Object document() {
            Object value = value();
            skipWhitespace();
            if (position != json.length()) {
                throw refusal("text after the end of the document");
            }
            return value;
        }

        private Object value() {
            skipWhitespace();
            if (position >= json.length()) {
                throw refusal("a value is missing");
            }
            char character = json.charAt(position);
            return switch (character) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> text();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            Map<String, Object> object = new LinkedHashMap<>();
            position++;
            skipWhitespace();
            if (consume('}')) {
                return object;
            }
            do {
                skipWhitespace();
                String key = text();
                skipWhitespace();
                expect(':');
                object.put(key, value());
                skipWhitespace();
            } while (consume(','));
            expect('}');
            return object;
        }

        private List<Object> array() {
            List<Object> array = new ArrayList<>();
            position++;
            skipWhitespace();
            if (consume(']')) {
                return array;
            }
            do {
                array.add(value());
                skipWhitespace();
            } while (consume(','));
            expect(']');
            return array;
        }

        private String text() {
            expect('"');
            StringBuilder text = new StringBuilder();
            while (position < json.length()) {
                char character = json.charAt(position++);
                if (character == '"') {
                    return text.toString();
                }
                if (character != '\\') {
                    text.append(character);
                    continue;
                }
                if (position >= json.length()) {
                    break;
                }
                char escaped = json.charAt(position++);
                switch (escaped) {
                    case 'n' -> text.append('\n');
                    case 'r' -> text.append('\r');
                    case 't' -> text.append('\t');
                    case 'b' -> text.append('\b');
                    case 'f' -> text.append('\f');
                    case 'u' -> text.append(unicodeEscape());
                    default -> text.append(escaped);
                }
            }
            throw refusal("a text is never closed");
        }

        /** The character of a unicode escape, its four hexadecimal digits next. */
        private char unicodeEscape() {
            if (position + 4 > json.length()) {
                throw refusal("a \\u escape has four hexadecimal digits");
            }
            try {
                char character = (char) Integer.parseInt(json.substring(position, position + 4), 16);
                position += 4;
                return character;
            } catch (NumberFormatException notHexadecimal) {
                throw refusal("a \\u escape has four hexadecimal digits");
            }
        }

        private Double number() {
            int start = position;
            while (position < json.length() && "+-0123456789.eE".indexOf(json.charAt(position)) >= 0) {
                position++;
            }
            if (start == position) {
                throw refusal("'" + json.charAt(position) + "' does not start a value");
            }
            try {
                return Double.parseDouble(json.substring(start, position));
            } catch (NumberFormatException notANumber) {
                throw refusal("'" + json.substring(start, position) + "' is not a number");
            }
        }

        private Object literal(String word, Object value) {
            if (!json.startsWith(word, position)) {
                throw refusal("expected " + word);
            }
            position += word.length();
            return value;
        }

        private void skipWhitespace() {
            while (position < json.length() && Character.isWhitespace(json.charAt(position))) {
                position++;
            }
        }

        private boolean consume(char character) {
            if (position < json.length() && json.charAt(position) == character) {
                position++;
                return true;
            }
            return false;
        }

        private void expect(char character) {
            if (!consume(character)) {
                throw refusal("expected '" + character + "'");
            }
        }

        private IllegalArgumentException refusal(String detail) {
            return new IllegalArgumentException("The symbol drawing is not valid JSON at " + position + ": " + detail + ".");
        }
    }
}
