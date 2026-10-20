package org.jmouse.kicad.connectivity;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.schematic.KiCadSymbolInstance;
import org.jmouse.kicad.symbol.KiCadPin;

/**
 * One pin of one placed symbol, in one instance of its sheet: what a net joins.
 *
 * @param sheetPath where the sheet instance stands in the design: {@code /root-uuid/sheet-uuid}
 * @param reference the symbol's reference in that instance: {@code R5}
 * @param unit      the unit's letter when the symbol has several — {@code A} of {@code U5A} —, else empty
 * @param symbol    the placed symbol
 * @param pin       the pin of its library symbol
 * @param at        where the pin connects on the sheet
 */
public record KiCadConnection(String sheetPath, String reference, String unit, KiCadSymbolInstance symbol, KiCadPin pin,
                              KiCadPoint at) {

    /** The reference as KiCad shows it on a net: with its unit's letter, {@code U5A}. */
    public String shownReference() {
        return reference + unit;
    }

    /** As KiCad names a pin on a net: {@code R5-1}. */
    @Override
    public String toString() {
        return reference + "-" + pin.number();
    }
}
