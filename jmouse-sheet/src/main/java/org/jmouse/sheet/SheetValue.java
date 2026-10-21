package org.jmouse.sheet;

import org.jmouse.units.Quantity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * What a field of a sheet document holds, as written. Each knows how sheet writes it, so a document read and written again
 * says the same.
 */
public sealed interface SheetValue {

    /** The value as sheet writes it. */
    String written();

    /** Words in quotes: {@code "8-bit shift register"}. */
    record Text(String text) implements SheetValue {

        @Override
        public String written() {
            return quoted(text);
        }
    }

    /** A number with, at most, its unit: {@code 6 V}, {@code 2.2 µs}, {@code -40 °C}, {@code 25}. */
    record Measure(Quantity quantity) implements SheetValue {

        @Override
        public String written() {
            return quantity.written();
        }
    }

    /** A word written bare: {@code resistor}, {@code push-pull}, {@code 74hc595}. */
    record Word(String word) implements SheetValue {

        @Override
        public String written() {
            return word;
        }
    }

    /**
     * Where another document's entry, or one of its fields, is: {@code 74hc595.sht#supply-voltage},
     * {@code 74hc595.sht#supply-voltage/maximum}. The document is named relative to the one that links.
     *
     * @param field the field's name; empty for the entry itself
     */
    record Link(String document, String id, String field) implements SheetValue {

        public boolean toField() {
            return !field.isEmpty();
        }

        @Override
        public String written() {
            return document + "#" + id + (field.isEmpty() ? "" : "/" + field);
        }
    }

    /** From one value to another: {@code 2 V .. 6 V}. */
    record Range(SheetValue from, SheetValue to) implements SheetValue {

        @Override
        public String written() {
            return from.written() + " .. " + to.written();
        }
    }

    /** A name given a value: {@code 14 = data} in a pin map. */
    record Assignment(String name, SheetValue value) implements SheetValue {

        @Override
        public String written() {
            return name + " = " + value.written();
        }
    }

    /** Values separated by commas: {@code 1 = a, 2 = k}. */
    record Sequence(List<SheetValue> items) implements SheetValue {

        public Sequence {
            items = List.copyOf(items);
        }

        @Override
        public String written() {
            return items.stream().map(SheetValue::written).collect(Collectors.joining(", "));
        }
    }

    /** Words as sheet quotes them: a quote and a backslash escaped. */
    static String quoted(String words) {
        return "\"" + words.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
