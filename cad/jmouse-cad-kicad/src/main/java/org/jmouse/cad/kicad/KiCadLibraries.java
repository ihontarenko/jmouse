package org.jmouse.cad.kicad;

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
 * What a drawing converted from KiCad's own libraries says of itself, as {@code kicad-libraries.sht} beside this class
 * writes it: who holds KiCad's libraries and under what terms, and which of our layers each KiCad layer is.
 *
 * @param holder  who holds the rights to KiCad's own libraries, named on every document converted from them
 * @param license their licence, as an SPDX expression
 * @param home    where they are kept, which a part fetched from them links to
 * @param category      the category a converted drawing is filed in
 * @param symbolTags    the tags a converted symbol carries
 * @param footprintTags the tags a converted footprint carries
 * @param componentTags the tags a component made from a KiCad symbol carries
 * @param circuitTags   the tags a sheet's circuit carries
 * @param span          how long an imported sheet's transient runs until its author says, seconds
 * @param ownLibrary    the KiCad library a component with no KiCad counterpart is written under
 * @param markSymbols   the symbol each mark of a sheet is drawn with, by the mark: {@code ground} is {@code ground-symbol}
 * @param layers        our layer each KiCad layer is, by KiCad's name: {@code F.SilkS} is {@code silkscreen}
 */
public record KiCadLibraries(String holder, String license, String home, String category, List<String> symbolTags,
                             List<String> footprintTags, List<String> componentTags, List<String> circuitTags, double span,
                             String ownLibrary, Map<String, String> markSymbols, Map<String, String> layers) {

    private static final String RESOURCE = "kicad-libraries.sht";

    public KiCadLibraries {
        symbolTags = List.copyOf(symbolTags);
        footprintTags = List.copyOf(footprintTags);
        componentTags = List.copyOf(componentTags);
        circuitTags = List.copyOf(circuitTags);
        markSymbols = Map.copyOf(markSymbols);
        layers = Map.copyOf(layers);
    }

    /** The table this library ships. */
    public static KiCadLibraries standard() {
        try (InputStream stream = KiCadLibraries.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("'" + RESOURCE + "' is not beside " + KiCadLibraries.class.getName());
            }
            return of(SheetReader.read(RESOURCE, new String(stream.readAllBytes(), StandardCharsets.UTF_8)));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    public static KiCadLibraries of(Sheet table) {
        Map<String, String> layers = new LinkedHashMap<>();
        for (SheetEntry entry : table.entries()) {
            entry.value("kicad").ifPresent(value -> items(value).forEach(kicad -> layers.put(text(kicad), entry.id())));
        }
        return new KiCadLibraries(text(table, "holder"), text(table, "license"), text(table, "home"), text(table, "category"),
                words(table, "symbol-tags"), words(table, "footprint-tags"), words(table, "component-tags"), words(table, "circuit-tags"),
                table.field("transient").map(field -> ((SheetValue.Measure) field.value()).quantity().value())
                        .orElseThrow(() -> new IllegalStateException("The KiCad table writes no transient")),
                text(table, "own-library"), assignments(table, "mark-symbols"), layers);
    }

    /** Our layer a KiCad layer is; empty for one that is not a part's. */
    public Optional<String> layerOf(String kicad) {
        return Optional.ofNullable(layers.get(kicad));
    }

    private static String text(Sheet table, String field) {
        return table.field(field).map(written -> text(written.value()))
                .orElseThrow(() -> new IllegalStateException("The KiCad table writes no " + field));
    }

    private static List<String> words(Sheet table, String field) {
        return table.field(field).map(written -> items(written.value()).stream().map(KiCadLibraries::text).toList())
                .orElse(List.of());
    }

    private static Map<String, String> assignments(Sheet table, String field) {
        Map<String, String> assigned = new LinkedHashMap<>();
        table.field(field).ifPresent(written -> items(written.value()).forEach(item -> {
            if (item instanceof SheetValue.Assignment assignment) {
                assigned.put(assignment.name(), text(assignment.value()));
            }
        }));
        return assigned;
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
