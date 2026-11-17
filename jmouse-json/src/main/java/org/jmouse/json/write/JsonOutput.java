package org.jmouse.json.write;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.el.writer.TextSourceWriter;
import org.jmouse.json.JsonInclusion;
import org.jmouse.json.JsonLayout;
import org.jmouse.json.JsonTexts;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 🧾 Where a {@link JsonSerializer} writes: one value — a scalar, an {@link #object object} of members or an {@link #array array}
 * of elements —, on the engine's {@link SourceWriter} in the layout asked for. A value it does not write itself it hands to
 * {@link #value(Object)}, which finds the serializer of its type.
 *
 * <p>Commas, colons, brackets and lines are the output's: a serializer says what its parts are, never how they are marked.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonOutput {

    private final SourceWriter    writer;
    private final JsonLayout      layout;
    private final JsonInclusion   inclusion;
    private final JsonSerializers serializers;

    private JsonOutput(SourceWriter writer, JsonLayout layout, JsonInclusion inclusion, JsonSerializers serializers) {
        this.writer = writer;
        this.layout = layout;
        this.inclusion = inclusion;
        this.serializers = serializers;
    }

    /** The text {@code body} writes to an output of this layout, inclusion and serializers. */
    public static String text(JsonLayout layout, JsonInclusion inclusion, JsonSerializers serializers, Consumer<JsonOutput> body) {
        TextSourceWriter writer = new TextSourceWriter(layout.sourceLayout());
        body.accept(new JsonOutput(writer, layout, inclusion, serializers));
        return writer.text();
    }

    /** {@code value}, written by the serializer of its type. */
    public void value(Object value) {
        if (value == null) {
            nullValue();
        } else {
            serializers.serialize(value, this);
        }
    }

    public void nullValue() {
        writer.word("null");
    }

    public void truth(boolean value) {
        writer.word(Boolean.toString(value));
    }

    public void text(String value) {
        writer.word(JsonTexts.encode(value));
    }

    /** A whole number. */
    public void number(long value) {
        writer.number(value);
    }

    /** A number in its shortest exact digits; one JSON cannot write — not a number, an infinity — is {@code null}, as JavaScript writes it. */
    public void number(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            nullValue();
        } else {
            writer.number(value);
        }
    }

    public void number(BigDecimal value) {
        writer.number(value);
    }

    public void number(BigInteger value) {
        writer.word(value.toString());
    }

    /** An object: its members, as {@code members} gives them. */
    public void object(Consumer<Members> members) {
        Members object = new Members();
        members.accept(object);
        container("{", "}", object.items);
    }

    /** An array: its elements, as {@code elements} gives them. */
    public void array(Consumer<Elements> elements) {
        Elements array = new Elements();
        elements.accept(array);
        container("[", "]", array.items);
    }

    /** Which members are written. */
    public JsonInclusion inclusion() {
        return inclusion;
    }

    /**
     * Items between their marks, a comma after each but the last: on one line in the compact layout, an item a line, indented,
     * in the other. An empty container is its two marks.
     */
    private void container(String opening, String closing, List<Consumer<JsonOutput>> items) {
        if (items.isEmpty()) {
            writer.word(opening + closing);
            return;
        }

        if (layout == JsonLayout.COMPACT) {
            writer.opening(opening);
            writeItems(items, false);
            writer.join(closing);
            return;
        }

        writer.block(opening, closing, ignored -> writeItems(items, true));
    }

    private void writeItems(List<Consumer<JsonOutput>> items, boolean aLineEach) {
        for (int index = 0; index < items.size(); index++) {
            items.get(index).accept(this);
            if (index < items.size() - 1) {
                writer.join(",");
            }
            if (aLineEach) {
                writer.endLine();
            }
        }
    }

    /** An object's members, given in order. */
    public final class Members {

        private final List<Consumer<JsonOutput>> items = new ArrayList<>();

        private Members() {
        }

        /** A member holding {@code value}, written by the serializer of its type; one holding null as the inclusion says. */
        public Members member(String name, Object value) {
            if (value == null && inclusion == JsonInclusion.NON_NULL) {
                return this;
            }
            return member(name, output -> output.value(value));
        }

        /** A member whose value {@code value} writes. */
        public Members member(String name, Consumer<JsonOutput> value) {
            items.add(output -> {
                writer.word(JsonTexts.encode(name)).join(":");
                value.accept(output);
            });
            return this;
        }
    }

    /** An array's elements, given in order. */
    public final class Elements {

        private final List<Consumer<JsonOutput>> items = new ArrayList<>();

        private Elements() {
        }

        /** An element, written by the serializer of its type. */
        public Elements element(Object value) {
            return element(output -> output.value(value));
        }

        /** An element that {@code value} writes. */
        public Elements element(Consumer<JsonOutput> value) {
            items.add(value);
            return this;
        }
    }
}
