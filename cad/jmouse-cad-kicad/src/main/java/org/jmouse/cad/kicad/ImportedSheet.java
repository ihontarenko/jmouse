package org.jmouse.cad.kicad;

import org.jmouse.cad.exchange.imported.ImportedCircuit;
import org.jmouse.kicad.symbol.KiCadSymbolDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * One KiCad sheet as an import reads it: the sheet itself, as every import reads one ({@link ImportedCircuit}), and what
 * only a KiCad sheet brings — the KiCad symbols it draws with and makes components of, and each part's KiCad footprint.
 *
 * @param symbols    the units of KiCad symbols it draws with KiCad's own drawing
 * @param components the KiCad symbols its parts are made from, where the library map names no component
 * @param footprints each part's KiCad footprint, by the part's reference: the library id its {@code Footprint} names
 * @param identifiers the id each symbol's and each sheet box's uuid became here: a board finds its parts by those uuids
 * @param pads       the pin each pad takes on KiCad's own footprints, by the part's id then the pad: KiCad's pads are its
 *                   symbol's pin numbers
 */
public record ImportedSheet(ImportedCircuit circuit, Set<SymbolUse> symbols, Set<KiCadSymbolDefinition> components,
                            Map<String, String> footprints, Map<String, String> identifiers,
                            Map<String, Map<String, String>> pads) {

    public ImportedSheet {
        symbols = Collections.unmodifiableSet(new LinkedHashSet<>(symbols));
        components = Collections.unmodifiableSet(new LinkedHashSet<>(components));
        footprints = Collections.unmodifiableMap(new LinkedHashMap<>(footprints));
        identifiers = Collections.unmodifiableMap(new LinkedHashMap<>(identifiers));
        pads = Collections.unmodifiableMap(new LinkedHashMap<>(pads));
    }

    /** A unit of a KiCad symbol the sheet draws with KiCad's own drawing, its pins named as the part names them. */
    public record SymbolUse(KiCadSymbolDefinition symbol, int unit, Map<String, String> pinNames) {
    }
}
