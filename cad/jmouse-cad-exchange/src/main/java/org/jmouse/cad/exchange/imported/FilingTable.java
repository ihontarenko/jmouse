package org.jmouse.cad.exchange.imported;

import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;
import org.jmouse.sheet.SheetReader;
import org.jmouse.sheet.SheetValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The table a product's import files its documents by ({@code kicad-libraries.sht}, {@code eagle-libraries.sht}), read
 * field by field: its texts, its lists of words, its assignments, its measures, and which of our layers each of the
 * product's layers is.
 */
public final class FilingTable {

    private final String name;
    private final Sheet  table;

    private FilingTable(String name, Sheet table) {
        this.name = name;
        this.table = table;
    }

    /** The table shipped beside a class. */
    public static FilingTable shipped(Class<?> beside, String resource) {
        try (InputStream stream = beside.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("'" + resource + "' is not beside " + beside.getName());
            }
            return of(resource, SheetReader.read(resource, new String(stream.readAllBytes(), StandardCharsets.UTF_8)));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    public static FilingTable of(String name, Sheet table) {
        return new FilingTable(name, table);
    }

    /** A field the table must write, as text. */
    public String text(String field) {
        return table.field(field).map(written -> text(written.value()))
                .orElseThrow(() -> new IllegalStateException("The table " + name + " writes no " + field));
    }

    /** A field's words, none when it writes none. */
    public List<String> words(String field) {
        return table.field(field).map(written -> items(written.value()).stream().map(FilingTable::text).toList())
                .orElse(List.of());
    }

    /** A field's {@code name = value} pairs, in order. */
    public Map<String, String> assignments(String field) {
        Map<String, String> assigned = new LinkedHashMap<>();
        table.field(field).ifPresent(written -> items(written.value()).forEach(item -> {
            if (item instanceof SheetValue.Assignment assignment) {
                assigned.put(assignment.name(), text(assignment.value()));
            }
        }));
        return assigned;
    }

    /** A measure the table must write, in its unit's base: {@code 20 ms} is 0.02. */
    public double measure(String field) {
        return table.field(field).map(written -> ((SheetValue.Measure) written.value()).quantity().value())
                .orElseThrow(() -> new IllegalStateException("The table " + name + " writes no " + field));
    }

    /** A length the table must write, in millimetres — the unit every drawing of a pcb is in —: {@code 5 µm} is 0.005. */
    public double millimetres(String field) {
        return measure(field) * MILLIMETRES_PER_METRE;
    }

    /** A length's base is the metre. */
    private static final double MILLIMETRES_PER_METRE = 1000;

    /** A measure the table may write; empty for one it does not. */
    public Optional<Double> optionalMeasure(String field) {
        return table.field(field).map(written -> ((SheetValue.Measure) written.value()).quantity().value());
    }

    /**
     * Our layer each of the product's layers is: every entry of the table names our layer by its id and the product's
     * by its {@code key} ({@code kicad "F.SilkS", "B.SilkS"}).
     */
    public Map<String, String> layers(String key) {
        Map<String, String> layers = new LinkedHashMap<>();
        for (SheetEntry entry : table.entries()) {
            entry.value(key).ifPresent(value -> items(value).forEach(product -> layers.put(text(product), entry.id())));
        }
        return layers;
    }

    private static List<SheetValue> items(SheetValue value) {
        return value instanceof SheetValue.Sequence sequence ? sequence.items() : List.of(value);
    }

    private static String text(SheetValue value) {
        return switch (value) {
            case SheetValue.Text text -> text.text();
            case SheetValue.Word word -> word.word();
            default -> value.written();
        };
    }
}
