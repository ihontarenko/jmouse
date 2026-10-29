package org.jmouse.cad.kicad;

import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetReader;
import org.jmouse.sheet.SheetEntry;
import org.jmouse.sheet.SheetValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which jMX component a KiCad symbol stands for, read from a sheet library map: {@code symbol "Device:R"}, its
 * {@code component}, which of its pins is which ({@code pins 1 = a, 2 = b}) and the parameter its value sets
 * ({@code value resistance}). An entry with no component but a symbol {@code code} names only how the part is drawn:
 * it stays a part without a model, drawn with the catalogue symbol of that code ({@code code "SY.DIODE:RECTIFIER"}) — the
 * identity a symbol keeps when its slug is renamed —, its pins named as the map names them. A symbol the map does not
 * name is imported as a part without a model — never guessed.
 *
 * <p>A <em>family</em> entry covers every count of a symbol written once: {@code symbol "Connector_Generic:Conn_01x{count}"},
 * {@code code "SY.HDR:1X{count}"}, {@code pin-names "pin{number}"} — the count read from the library id
 * ({@code 01x04} is 4), a numbered pin named by the template, a pin written in {@code pins} as written. An entry naming
 * the id exactly is asked first.</p>
 *
 * <p>A <em>footprint</em> entry pairs a KiCad footprint with one of this library's by its code — the identity a footprint
 * keeps when its slug is renamed —, both ways: {@code footprint "Resistor_SMD:R_0603_1608Metric"},
 * {@code code "FP.CHIP:R0603"}. An import lands a part naming the
 * KiCad id on ours — nothing converted, no copy of a package the library already draws —, and an export writes ours as
 * the KiCad id, so the design opens in KiCad on its own libraries. Pads are numbered alike on both sides.</p>
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
     * @param code the catalogue symbol's code ({@code meta { code … }})
     * @param pins the symbol's pin by the KiCad pin's number
     */
    public record Drawing(String code, Map<String, String> pins) {

        public Drawing {
            pins = Map.copyOf(pins);
        }
    }

    public static final KiCadLibraryMap EMPTY = new KiCadLibraryMap(Map.of(), Map.of(), List.of(), Map.of(), Set.of());

    private static final String COUNT = "{count}";
    private static final String NUMBER = "{number}";

    /**
     * A drawing for every count of a library id written with {@code {count}}.
     *
     * @param pattern  the library id with the count as its one group
     * @param code     the symbol's code, {@code {count}} in it
     * @param pinNames how a numbered pin is named, {@code {number}} in it
     * @param pins     pins named as written — a shield's {@code SH}
     */
    private record Family(Pattern pattern, String code, String pinNames, Map<String, String> pins) {

        Optional<Drawing> drawingOf(String libraryId) {
            Matcher matcher = pattern.matcher(libraryId);
            if (!matcher.matches()) {
                return Optional.empty();
            }
            int count = Integer.parseInt(matcher.group(1));
            Map<String, String> named = new LinkedHashMap<>();
            // A row of n has n pins, two rows 2n: every number a part of this count can have is named.
            for (int number = 1; number <= 2 * count; number++) {
                named.put(String.valueOf(number), pinNames.replace(NUMBER, String.valueOf(number)));
            }
            named.putAll(pins);
            return Optional.of(new Drawing(code.replace(COUNT, String.valueOf(count)), named));
        }
    }

    private final Map<String, Mapping> mappings;
    private final Map<String, Drawing> drawings;
    private final List<Family> families;
    private final Map<String, String> footprints;
    private final Set<String> boxedDesignators;

    private KiCadLibraryMap(Map<String, Mapping> mappings, Map<String, Drawing> drawings, List<Family> families,
                            Map<String, String> footprints, Set<String> boxedDesignators) {
        this.boxedDesignators = Set.copyOf(boxedDesignators);
        // In the order written: a component two symbols stand for is exported as the first.
        this.mappings = Collections.unmodifiableMap(new LinkedHashMap<>(mappings));
        this.drawings = Map.copyOf(drawings);
        this.families = List.copyOf(families);
        // By KiCad id, in the order written: a code two KiCad ids stand for is exported as the first.
        this.footprints = Collections.unmodifiableMap(new LinkedHashMap<>(footprints));
    }

    /** Where the map this library ships stands, beside this class. */
    private static final String RESOURCE = "kicad-library-map.sht";

    /** The map this library ships: which of its components each symbol of KiCad's standard libraries stands for. */
    public static KiCadLibraryMap standard() {
        try (InputStream stream = KiCadLibraryMap.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("'" + RESOURCE + "' is not beside " + KiCadLibraryMap.class.getName());
            }
            return of(SheetReader.read(RESOURCE, new String(stream.readAllBytes(), StandardCharsets.UTF_8)));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    /** The map a sheet document holds, entry by entry. */
    public static KiCadLibraryMap of(Sheet map) {
        Map<String, Mapping> mappings = new LinkedHashMap<>();
        Map<String, Drawing> drawings = new LinkedHashMap<>();
        List<Family> families = new ArrayList<>();
        Map<String, String> footprints = new LinkedHashMap<>();
        for (SheetEntry entry : map.entries()) {
            Optional<String> footprint = entry.value("footprint").map(KiCadLibraryMap::word);
            if (footprint.isPresent()) {
                String code = entry.value("code").map(KiCadLibraryMap::word).orElseThrow(() ->
                        new IllegalArgumentException("The footprint '" + entry.id() + "' names no code"));
                footprints.put(footprint.get(), code);
                continue;
            }
            Optional<String> symbol = entry.value("symbol").map(KiCadLibraryMap::word);
            if (symbol.isEmpty()) {
                continue;
            }
            Map<String, String> pins = new LinkedHashMap<>();
            entry.value("pins").ifPresent(written -> assignments(written).forEach(assignment ->
                    pins.put(assignment.name(), word(assignment.value()))));
            if (symbol.get().contains(COUNT)) {
                String[] around = symbol.get().split(Pattern.quote(COUNT), -1);
                Pattern pattern = Pattern.compile(Pattern.quote(around[0]) + "(\\d+)" + Pattern.quote(around[1]));
                String code = entry.value("code").map(KiCadLibraryMap::word).orElseThrow(() ->
                        new IllegalArgumentException("The family '" + entry.id() + "' names no symbol code"));
                String pinNames = entry.value("pin-names").map(KiCadLibraryMap::word).orElse(NUMBER);
                families.add(new Family(pattern, code, pinNames, pins));
                continue;
            }
            Optional<String> component = entry.value("component").map(KiCadLibraryMap::word);
            if (component.isPresent()) {
                mappings.put(symbol.get(), new Mapping(component.get(), pins, entry.value("value").map(KiCadLibraryMap::word)));
            } else {
                entry.value("code").map(KiCadLibraryMap::word)
                        .ifPresent(code -> drawings.put(symbol.get(), new Drawing(code, pins)));
            }
        }
        Set<String> boxed = new LinkedHashSet<>();
        map.field("boxed-designators").ifPresent(field -> (field.value() instanceof SheetValue.Sequence sequence
                ? sequence.items() : List.of(field.value())).forEach(item -> boxed.add(word(item))));
        return new KiCadLibraryMap(mappings, drawings, families, footprints, boxed);
    }

    /** The code of this library's footprint a KiCad footprint's library id stands for, when the map pairs them: the way in. */
    public Optional<String> footprintCodeOf(String libraryId) {
        return Optional.ofNullable(footprints.get(libraryId));
    }

    /** The KiCad library id the footprint of this code is written as — the first entry pairing it —: the way back. */
    public Optional<String> footprintIdOf(String code) {
        return footprints.entrySet().stream().filter(entry -> entry.getValue().equals(code))
                .map(Map.Entry::getKey).findFirst();
    }

    /**
     * The library id a component is written as in KiCad — the first entry that names it —, when the map names one: the
     * way back an export takes.
     */
    public Optional<String> libraryIdOf(String component) {
        return mappings.entrySet().stream().filter(entry -> entry.getValue().component().equals(component))
                .map(Map.Entry::getKey).findFirst();
    }

    /** The designators of chips — {@code U}, {@code IC} —, which a design's symbol of are drawn as our box. */
    public Set<String> boxedDesignators() {
        return boxedDesignators;
    }

    /** What a library id stands for, when the map names it. */
    public Optional<Mapping> mappingOf(String libraryId) {
        return Optional.ofNullable(mappings.get(libraryId));
    }

    /** How a library id with no model is drawn, when the map names a drawing for it. */
    public Optional<Drawing> drawingOf(String libraryId) {
        Drawing exact = drawings.get(libraryId);
        if (exact != null) {
            return Optional.of(exact);
        }
        return families.stream().map(family -> family.drawingOf(libraryId)).flatMap(Optional::stream).findFirst();
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
