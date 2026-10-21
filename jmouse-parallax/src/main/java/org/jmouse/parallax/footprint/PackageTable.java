package org.jmouse.parallax.footprint;

import org.jmouse.datamap.DataMapEntry;
import org.jmouse.datamap.DataMapValue;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One entry of a package table — a JEDEC outline, a chip size, a header pitch — read in millimetres. A length is written
 * with its unit ({@code 1.27 mm}); a tolerance as a range ({@code 5.8 mm .. 6.2 mm}).
 */
record PackageTable(DataMapEntry entry) {

    /** From the smallest to the largest a maker may ship. */
    record Span(double minimum, double maximum) {

        double nominal() {
            return (minimum + maximum) / 2;
        }
    }

    String id() {
        return entry.id();
    }

    String title() {
        return entry.title().orElse(entry.id());
    }

    /** The standard the entry was read from: its {@code source standard "…"}, or empty. */
    String standard() {
        return entry.source().flatMap(source -> source.part("standard").or(() -> source.part("table"))).orElse("");
    }

    double length(String field) {
        return millimetres(require(field), field);
    }

    double number(String field) {
        if (require(field) instanceof DataMapValue.Measure(Quantity quantity)) {
            return quantity.value();
        }
        throw refused(field, "a number");
    }

    Span span(String field) {
        DataMapValue value = require(field);
        if (value instanceof DataMapValue.Range(DataMapValue from, DataMapValue to)) {
            return new Span(millimetres(from, field), millimetres(to, field));
        }
        double single = millimetres(value, field);
        return new Span(single, single);
    }

    String word(String field) {
        return optionalWord(field).orElseThrow(() -> refused(field, "a word"));
    }

    Optional<String> optionalWord(String field) {
        return entry.value(field).map(value -> switch (value) {
            case DataMapValue.Word(String word) -> word;
            case DataMapValue.Text(String text) -> text;
            default -> throw refused(field, "a word");
        });
    }

    /** {@code members 8 = 4.9 mm, 14 = 8.65 mm}: each lead count with its body length, in the order written. */
    Map<Integer, Double> members(String field) {
        Map<Integer, Double> members = new LinkedHashMap<>();
        for (DataMapValue item : items(require(field))) {
            if (!(item instanceof DataMapValue.Assignment(String name, DataMapValue length))) {
                throw refused(field, "lead counts with their lengths: 8 = 4.9 mm");
            }
            members.put(Integer.valueOf(name), millimetres(length, field));
        }
        return members;
    }

    /** {@code rows 1, 2} or {@code pins 1 .. 40}: whole numbers, a range spelled out. */
    List<Integer> counts(String field) {
        DataMapValue value = require(field);
        List<Integer> counts = new ArrayList<>();
        if (value instanceof DataMapValue.Range(DataMapValue from, DataMapValue to)) {
            for (int count = whole(from, field); count <= whole(to, field); count++) {
                counts.add(count);
            }
            return counts;
        }
        for (DataMapValue item : items(value)) {
            counts.add(whole(item, field));
        }
        return counts;
    }

    private DataMapValue require(String field) {
        return entry.value(field).orElseThrow(() -> new IllegalArgumentException(
                "The package table entry '" + entry.id() + "' has no '" + field + "'"));
    }

    private static List<DataMapValue> items(DataMapValue value) {
        return value instanceof DataMapValue.Sequence(List<DataMapValue> items) ? items : List.of(value);
    }

    private double millimetres(DataMapValue value, String field) {
        if (value instanceof DataMapValue.Measure(Quantity quantity) && "m".equals(quantity.unit())) {
            return Math.round(quantity.value() * 1e6) / 1e3;
        }
        throw refused(field, "a length in millimetres");
    }

    private int whole(DataMapValue value, String field) {
        if (value instanceof DataMapValue.Measure(Quantity quantity) && quantity.unit().isEmpty()
                && quantity.value() == Math.rint(quantity.value())) {
            return (int) quantity.value();
        }
        throw refused(field, "a whole number");
    }

    private IllegalArgumentException refused(String field, String expected) {
        return new IllegalArgumentException("The package table entry '" + entry.id() + "' writes '" + field
                + "' as something other than " + expected);
    }
}
