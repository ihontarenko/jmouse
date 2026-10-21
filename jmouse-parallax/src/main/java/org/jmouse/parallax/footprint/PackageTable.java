package org.jmouse.parallax.footprint;

import org.jmouse.datamap.DataMapEntry;
import org.jmouse.datamap.DataMapField;
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

    /**
     * A size written inside a kind: its own fields first, then the kind's — {@code name}, {@code folder}, the pitches —,
     * so what every size of a kind shares is written once.
     */
    static PackageTable inheriting(DataMapEntry kind, DataMapEntry size) {
        List<DataMapField> fields = new ArrayList<>(size.fields());
        fields.addAll(kind.fields());
        return new PackageTable(new DataMapEntry(size.id(), size.title(), size.source().or(kind::source), fields,
                size.entries()));
    }

    /** A footprint's name from its kind's template: {@code "axial-resistor-body-{code}-pitch-{pitch}"}. */
    static String named(String template, Map<String, String> words) {
        String named = template;
        for (Map.Entry<String, String> word : words.entrySet()) {
            named = named.replace("{" + word.getKey() + "}", word.getValue());
        }
        if (named.contains("{")) {
            throw new IllegalArgumentException("The name template '" + template + "' names a size no table gives: " + named);
        }
        return named;
    }

    /** A length as a name writes it: hundredths of a millimetre, at least three digits — 2.54 → 254, 0.5 → 050. */
    static String hundredths(double millimetres) {
        String written = String.valueOf(Math.round(millimetres * 100));
        return "0".repeat(Math.max(0, 3 - written.length())) + written;
    }

    /**
     * What a name template may use: every field written as a single length, in hundredths of a millimetre, and every
     * field written as a word or a text, as written.
     */
    Map<String, String> words() {
        Map<String, String> words = new LinkedHashMap<>();
        for (DataMapField field : entry.fields()) {
            switch (field.value()) {
                case DataMapValue.Measure(Quantity quantity) when "m".equals(quantity.unit()) ->
                        words.putIfAbsent(field.name(), hundredths(Math.round(quantity.value() * 1e6) / 1e3));
                case DataMapValue.Word(String word) -> words.putIfAbsent(field.name(), word);
                case DataMapValue.Text(String text) -> words.putIfAbsent(field.name(), text);
                default -> {
                }
            }
        }
        return words;
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

    /** {@code pitches 5.08 mm, 7.62 mm}: lengths in the order written. */
    List<Double> lengths(String field) {
        List<Double> lengths = new ArrayList<>();
        for (DataMapValue item : items(require(field))) {
            lengths.add(millimetres(item, field));
        }
        return lengths;
    }

    boolean has(String field) {
        return entry.value(field).isPresent();
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

    /** {@code left 1 = -1, 2 = 1}: each name with a plain number, in the order written. */
    Map<String, Double> places(String field) {
        Map<String, Double> places = new LinkedHashMap<>();
        for (DataMapValue item : items(require(field))) {
            if (!(item instanceof DataMapValue.Assignment(String name, DataMapValue.Measure(Quantity quantity)))
                    || !quantity.unit().isEmpty()) {
                throw refused(field, "names with plain numbers: 1 = -1, 2 = 1");
            }
            places.put(name, quantity.value());
        }
        return places;
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
