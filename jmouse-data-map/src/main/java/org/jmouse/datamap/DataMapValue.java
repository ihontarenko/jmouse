package org.jmouse.datamap;

import java.util.List;
import java.util.stream.Collectors;

/**
 * What a field of a jMD document holds, as written. Each knows how jMD writes it, so a document read and written again
 * says the same.
 */
public sealed interface DataMapValue {

    /** The value as jMD writes it. */
    String written();

    /** Words in quotes: {@code "8-bit shift register"}. */
    record Text(String text) implements DataMapValue {

        @Override
        public String written() {
            return quoted(text);
        }
    }

    /** A number with, at most, its unit: {@code 6 V}, {@code 2.2 µs}, {@code -40 °C}, {@code 25}. */
    record Measure(Quantity quantity) implements DataMapValue {

        @Override
        public String written() {
            return quantity.written();
        }
    }

    /** A word written bare: {@code resistor}, {@code push-pull}, {@code 74hc595}. */
    record Word(String word) implements DataMapValue {

        @Override
        public String written() {
            return word;
        }
    }

    /**
     * Where another document's entry, or one of its fields, is: {@code 74hc595.jmd#supply-voltage},
     * {@code 74hc595.jmd#supply-voltage/maximum}. The document is named relative to the one that links.
     *
     * @param field the field's name; empty for the entry itself
     */
    record Link(String document, String id, String field) implements DataMapValue {

        public boolean toField() {
            return !field.isEmpty();
        }

        @Override
        public String written() {
            return document + "#" + id + (field.isEmpty() ? "" : "/" + field);
        }
    }

    /** From one value to another: {@code 2 V .. 6 V}. */
    record Range(DataMapValue from, DataMapValue to) implements DataMapValue {

        @Override
        public String written() {
            return from.written() + " .. " + to.written();
        }
    }

    /** A name given a value: {@code 14 = data} in a pin map. */
    record Assignment(String name, DataMapValue value) implements DataMapValue {

        @Override
        public String written() {
            return name + " = " + value.written();
        }
    }

    /** Values separated by commas: {@code 1 = a, 2 = k}. */
    record Sequence(List<DataMapValue> items) implements DataMapValue {

        public Sequence {
            items = List.copyOf(items);
        }

        @Override
        public String written() {
            return items.stream().map(DataMapValue::written).collect(Collectors.joining(", "));
        }
    }

    /** Words as jMD quotes them: a quote and a backslash escaped. */
    static String quoted(String words) {
        return "\"" + words.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
