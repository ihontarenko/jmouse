package org.jmouse.parallax.kicad;

import org.jmouse.datamap.DataMap;
import org.jmouse.datamap.DataMapEntry;
import org.jmouse.datamap.DataMapValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Which jMX component a KiCad symbol stands for, read from a jMD library map: {@code symbol "Device:R"}, its
 * {@code component}, which of its pins is which ({@code pins 1 = a, 2 = b}) and the parameter its value sets
 * ({@code value resistance}). A symbol the map does not name is imported as a part without a model — never guessed.
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

    public static final KiCadLibraryMap EMPTY = new KiCadLibraryMap(Map.of());

    private final Map<String, Mapping> mappings;

    private KiCadLibraryMap(Map<String, Mapping> mappings) {
        this.mappings = Map.copyOf(mappings);
    }

    /** The map a jMD document holds, entry by entry. */
    public static KiCadLibraryMap of(DataMap map) {
        Map<String, Mapping> mappings = new LinkedHashMap<>();
        for (DataMapEntry entry : map.entries()) {
            Optional<String> symbol = entry.value("symbol").map(KiCadLibraryMap::word);
            Optional<String> component = entry.value("component").map(KiCadLibraryMap::word);
            if (symbol.isEmpty() || component.isEmpty()) {
                continue;
            }
            Map<String, String> pins = new LinkedHashMap<>();
            entry.value("pins").ifPresent(written -> assignments(written).forEach(assignment ->
                    pins.put(assignment.name(), word(assignment.value()))));
            mappings.put(symbol.get(), new Mapping(component.get(), pins, entry.value("value").map(KiCadLibraryMap::word)));
        }
        return new KiCadLibraryMap(mappings);
    }

    /** What a library id stands for, when the map names it. */
    public Optional<Mapping> mappingOf(String libraryId) {
        return Optional.ofNullable(mappings.get(libraryId));
    }

    private static List<DataMapValue.Assignment> assignments(DataMapValue value) {
        return switch (value) {
            case DataMapValue.Assignment assignment -> List.of(assignment);
            case DataMapValue.Sequence sequence -> sequence.items().stream()
                    .filter(DataMapValue.Assignment.class::isInstance).map(DataMapValue.Assignment.class::cast).toList();
            default -> List.of();
        };
    }

    private static String word(DataMapValue value) {
        return switch (value) {
            case DataMapValue.Text text -> text.text();
            case DataMapValue.Word word -> word.word();
            default -> value.written();
        };
    }
}
