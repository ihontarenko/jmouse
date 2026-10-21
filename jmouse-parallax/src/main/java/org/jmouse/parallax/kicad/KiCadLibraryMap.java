package org.jmouse.parallax.kicad;

import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;
import org.jmouse.sheet.SheetValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Which jMX component a KiCad symbol stands for, read from a sheet library map: {@code symbol "Device:R"}, its
 * {@code component}, which of its pins is which ({@code pins 1 = a, 2 = b}) and the parameter its value sets
 * ({@code value resistance}). An entry with no component but a {@code drawing} names only how the part is drawn: it
 * stays a part without a model, drawn with that catalogue symbol, its pins named as the map names them. A symbol the
 * map does not name is imported as a part without a model — never guessed.
 */
public final class KiCadLibraryMap {

    /**
     * What one KiCad symbol is.
     *
     * @param component the jMX component's slug
     * @param pins      the component's pin by the KiCad pin's number
     * @param value     the parameter the symbol's value sets; empty when its value is its name
     */
    public record Mapping(String component, Map<String, String> pins, Optional<String> value) {

        public Mapping {
            pins = Map.copyOf(pins);
        }
    }

    /**
     * How a KiCad symbol with no model is drawn.
     *
     * @param symbol the catalogue symbol's slug
     * @param pins   the symbol's pin by the KiCad pin's number
     */
    public record Drawing(String symbol, Map<String, String> pins) {

        public Drawing {
            pins = Map.copyOf(pins);
        }
    }

    public static final KiCadLibraryMap EMPTY = new KiCadLibraryMap(Map.of(), Map.of());

    private final Map<String, Mapping> mappings;
    private final Map<String, Drawing> drawings;

    private KiCadLibraryMap(Map<String, Mapping> mappings, Map<String, Drawing> drawings) {
        // In the order written: a component two symbols stand for is exported as the first.
        this.mappings = Collections.unmodifiableMap(new LinkedHashMap<>(mappings));
        this.drawings = Map.copyOf(drawings);
    }

    /** The map a sheet document holds, entry by entry. */
    public static KiCadLibraryMap of(Sheet map) {
        Map<String, Mapping> mappings = new LinkedHashMap<>();
        Map<String, Drawing> drawings = new LinkedHashMap<>();
        for (SheetEntry entry : map.entries()) {
            Optional<String> symbol = entry.value("symbol").map(KiCadLibraryMap::word);
            if (symbol.isEmpty()) {
                continue;
            }
            Map<String, String> pins = new LinkedHashMap<>();
            entry.value("pins").ifPresent(written -> assignments(written).forEach(assignment ->
                    pins.put(assignment.name(), word(assignment.value()))));
            Optional<String> component = entry.value("component").map(KiCadLibraryMap::word);
            if (component.isPresent()) {
                mappings.put(symbol.get(), new Mapping(component.get(), pins, entry.value("value").map(KiCadLibraryMap::word)));
            } else {
                entry.value("drawing").map(KiCadLibraryMap::word)
                        .ifPresent(drawing -> drawings.put(symbol.get(), new Drawing(drawing, pins)));
            }
        }
        return new KiCadLibraryMap(mappings, drawings);
    }

    /**
     * The library id a component is written as in KiCad — the first entry that names it —, when the map names one: the
     * way back an export takes.
     */
    public Optional<String> libraryIdOf(String component) {
        return mappings.entrySet().stream().filter(entry -> entry.getValue().component().equals(component))
                .map(Map.Entry::getKey).findFirst();
    }

    /** What a library id stands for, when the map names it. */
    public Optional<Mapping> mappingOf(String libraryId) {
        return Optional.ofNullable(mappings.get(libraryId));
    }

    /** How a library id with no model is drawn, when the map names a drawing for it. */
    public Optional<Drawing> drawingOf(String libraryId) {
        return Optional.ofNullable(drawings.get(libraryId));
    }

    private static List<SheetValue.Assignment> assignments(SheetValue value) {
        return switch (value) {
            case SheetValue.Assignment assignment -> List.of(assignment);
            case SheetValue.Sequence sequence -> sequence.items().stream()
                    .filter(SheetValue.Assignment.class::isInstance).map(SheetValue.Assignment.class::cast).toList();
            default -> List.of();
        };
    }

    private static String word(SheetValue value) {
        return switch (value) {
            case SheetValue.Text text -> text.text();
            case SheetValue.Word word -> word.word();
            default -> value.written();
        };
    }
}
