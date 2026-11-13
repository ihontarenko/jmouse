package org.jmouse.cad.kicad;

import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.jmouse.kicad.symbol.KiCadSymbolLibraries;

/**
 * KiCad's libraries installed beside the engine, as a design's import asks them: the footprints its parts are looked up in after
 * its own, and whose what a design brings is. A design keeps its own copy of every symbol it places, and may keep copies of
 * KiCad's footprint libraries among its own; such a copy is one of KiCad's — the KiCad Libraries Team's, under their licence —
 * when the installed library of its name holds an element of its name. Otherwise it is the design's own, naming no holder and
 * no terms.
 *
 * @param footprints KiCad's installed footprint libraries
 * @param symbols    KiCad's installed symbol libraries
 */
public record InstalledKiCadLibraries(KiCadFootprintLibraries footprints, KiCadSymbolLibraries symbols) {

    /** None installed: every footprint and symbol a design brings is its own. */
    public static final InstalledKiCadLibraries NONE = new InstalledKiCadLibraries(KiCadFootprintLibraries.NONE, KiCadSymbolLibraries.NONE);

    /** Whether a symbol a schematic names, {@code Transistor_FET:2N7002}, is one of KiCad's installed libraries'. */
    public boolean holdsSymbol(String libraryIdentifier) {
        return symbols.holds(libraryIdentifier);
    }

    /** Whether a footprint a schematic names, {@code Package_QFP:LQFP-64_10x10mm_P0.5mm}, is one of KiCad's installed libraries'. */
    public boolean holdsFootprint(String libraryIdentifier) {
        return footprints.originOf(libraryIdentifier).isPresent();
    }
}
