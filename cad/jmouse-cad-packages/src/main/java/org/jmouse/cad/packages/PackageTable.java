package org.jmouse.cad.packages;

import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;
import org.jmouse.sheet.SheetField;
import org.jmouse.sheet.SheetValue;
import org.jmouse.units.Quantity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One entry of a library table — a JEDEC outline, a chip size, a header pitch, a connector family — read in millimetres. A length is written
 * with its unit ({@code 1.27 mm}); a tolerance as a range ({@code 5.8 mm .. 6.2 mm}).
 */
public record PackageTable(SheetEntry entry) {

    /** From the smallest to the largest a maker may ship. */
    public record Span(double minimum, double maximum) {

        public double nominal() {
            return (minimum + maximum) / 2;
        }
    }

    /** A sheet's own fields — what it writes before its entries — read as a table: a sheet of rules. */
    public static PackageTable head(Sheet sheet) {
        return new PackageTable(new SheetEntry(sheet.slug(), Optional.of(sheet.title()), Optional.empty(), sheet.fields(), List.of()));
    }

    /**
     * A size written inside a kind: its own fields first, then the kind's — {@code name}, {@code folder}, the pitches —,
     * so what every size of a kind shares is written once.
     */
    public static PackageTable inheriting(SheetEntry kind, SheetEntry size) {
        List<SheetField> fields = new ArrayList<>(size.fields());
        fields.addAll(kind.fields());
        return new PackageTable(new SheetEntry(size.id(), size.title(), size.source().or(kind::source), fields,
                size.entries()));
    }

    /** A footprint's name from its kind's template: {@code "axial-resistor-body-{code}-pitch-{pitch}"}. */
    public static String named(String template, Map<String, String> words) {
        String named = template;
        for (Map.Entry<String, String> word : words.entrySet()) {
            named = named.replace("{" + word.getKey() + "}", word.getValue());
        }
        if (named.contains("{")) {
            throw new IllegalArgumentException("The name template '" + template + "' names a size no table gives: " + named);
        }
        return named;
    }

    /** A placeholder of a code template: a word's name, and what is done to it — {@code {pitch|mm}}, {@code {standing|initial}}. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-z-]+)(?:\\|([a-z]+))?}");

    /**
     * A package's designation from its kind's template — {@code "AXIAL:R{code}-P{pitch|mm}-{standing|initial}"} —, from the
     * same words its name is made of: the technical code of its footprint and of its portrait alike, once their class is
     * written before it. A word may be read through one filter: {@code mm} reads a length written
     * in hundredths back as millimetres ({@code 762} → {@code 7.62}), {@code initial} keeps its first letter, upper case
     * ({@code horizontal} → {@code H}), {@code upper} writes it upper case.
     */
    public static String designated(String template, Map<String, String> words) {
        Matcher placeholder = PLACEHOLDER.matcher(template);
        StringBuilder designated = new StringBuilder();
        while (placeholder.find()) {
            String word = words.get(placeholder.group(1));
            if (word == null) {
                throw new IllegalArgumentException("The code template '" + template + "' names a word no table gives: "
                        + placeholder.group(1));
            }
            placeholder.appendReplacement(designated, Matcher.quoteReplacement(filtered(word, placeholder.group(2), template)));
        }
        placeholder.appendTail(designated);
        return designated.toString();
    }

    private static String filtered(String word, String filter, String template) {
        if (filter == null) {
            return word;
        }
        return switch (filter) {
            case "mm" -> new BigDecimal(word).movePointLeft(2).stripTrailingZeros().toPlainString();
            case "initial" -> word.isEmpty() ? "" : word.substring(0, 1).toUpperCase(Locale.ROOT);
            case "upper" -> word.toUpperCase(Locale.ROOT);
            default -> throw new IllegalArgumentException("The code template '" + template + "' reads a word through '"
                    + filter + "'; there are mm, initial and upper");
        };
    }

    /** A length as a name writes it: hundredths of a millimetre, at least three digits — 2.54 → 254, 0.5 → 050. */
    public static String hundredths(double millimetres) {
        String written = String.valueOf(Math.round(millimetres * 100));
        return "0".repeat(Math.max(0, 3 - written.length())) + written;
    }

    /**
     * What a name template may use: every field written as a single length, in hundredths of a millimetre, and every
     * field written as a word or a text, as written.
     */
    public Map<String, String> words() {
        Map<String, String> words = new LinkedHashMap<>();
        for (SheetField field : entry.fields()) {
            switch (field.value()) {
                case SheetValue.Measure(Quantity quantity) when "m".equals(quantity.unit()) ->
                        words.putIfAbsent(field.name(), hundredths(Math.round(quantity.value() * 1e6) / 1e3));
                case SheetValue.Word(String word) -> words.putIfAbsent(field.name(), word);
                case SheetValue.Text(String text) -> words.putIfAbsent(field.name(), text);
                default -> {
                }
            }
        }
        return words;
    }

    public String id() {
        return entry.id();
    }

    public String title() {
        return entry.title().orElse(entry.id());
    }

    /** The standard the entry was read from: its {@code source standard "…"}, or empty. */
    public String standard() {
        return entry.source().flatMap(source -> source.part("standard").or(() -> source.part("table"))).orElse("");
    }

    public double length(String field) {
        return millimetres(require(field), field);
    }

    public double number(String field) {
        if (require(field) instanceof SheetValue.Measure(Quantity quantity)) {
            return quantity.value();
        }
        throw refused(field, "a number");
    }

    public Span span(String field) {
        SheetValue value = require(field);
        if (value instanceof SheetValue.Range(SheetValue from, SheetValue to)) {
            return new Span(millimetres(from, field), millimetres(to, field));
        }
        double single = millimetres(value, field);
        return new Span(single, single);
    }

    public String word(String field) {
        return optionalWord(field).orElseThrow(() -> refused(field, "a word"));
    }

    public Optional<String> optionalWord(String field) {
        return entry.value(field).map(value -> switch (value) {
            case SheetValue.Word(String word) -> word;
            case SheetValue.Text(String text) -> text;
            default -> throw refused(field, "a word");
        });
    }

    /** {@code pitches 5.08 mm, 7.62 mm}: lengths in the order written. */
    public List<Double> lengths(String field) {
        List<Double> lengths = new ArrayList<>();
        for (SheetValue item : items(require(field))) {
            lengths.add(millimetres(item, field));
        }
        return lengths;
    }

    public boolean has(String field) {
        return entry.value(field).isPresent();
    }

    /** {@code members 8 = 4.9 mm, 14 = 8.65 mm}: each lead count with its body length, in the order written. */
    public Map<Integer, Double> members(String field) {
        Map<Integer, Double> members = new LinkedHashMap<>();
        for (SheetValue item : items(require(field))) {
            if (!(item instanceof SheetValue.Assignment(String name, SheetValue length))) {
                throw refused(field, "lead counts with their lengths: 8 = 4.9 mm");
            }
            members.put(Integer.valueOf(name), millimetres(length, field));
        }
        return members;
    }

    /** {@code left 1 = -1, 2 = 1}: each name with a plain number, in the order written. */
    public Map<String, Double> places(String field) {
        Map<String, Double> places = new LinkedHashMap<>();
        for (SheetValue item : items(require(field))) {
            if (!(item instanceof SheetValue.Assignment(String name, SheetValue.Measure(Quantity quantity)))
                    || !quantity.unit().isEmpty()) {
                throw refused(field, "names with plain numbers: 1 = -1, 2 = 1");
            }
            places.put(name, quantity.value());
        }
        return places;
    }

    /** {@code rows 1, 2} or {@code pins 1 .. 40}: whole numbers, a range spelled out. */
    public List<Integer> counts(String field) {
        SheetValue value = require(field);
        List<Integer> counts = new ArrayList<>();
        if (value instanceof SheetValue.Range(SheetValue from, SheetValue to)) {
            for (int count = whole(from, field); count <= whole(to, field); count++) {
                counts.add(count);
            }
            return counts;
        }
        for (SheetValue item : items(value)) {
            counts.add(whole(item, field));
        }
        return counts;
    }

    private SheetValue require(String field) {
        return entry.value(field).orElseThrow(() -> new IllegalArgumentException(
                "The package table entry '" + entry.id() + "' has no '" + field + "'"));
    }

    private static List<SheetValue> items(SheetValue value) {
        return value instanceof SheetValue.Sequence(List<SheetValue> items) ? items : List.of(value);
    }

    private double millimetres(SheetValue value, String field) {
        if (value instanceof SheetValue.Measure(Quantity quantity) && "m".equals(quantity.unit())) {
            return Math.round(quantity.value() * 1e6) / 1e3;
        }
        throw refused(field, "a length in millimetres");
    }

    private int whole(SheetValue value, String field) {
        if (value instanceof SheetValue.Measure(Quantity quantity) && quantity.unit().isEmpty()
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
