package org.jmouse.kicad.connectivity;

import java.util.List;

/**
 * A net of a design: its name as KiCad names it, and the pins it joins.
 *
 * @param name    a power symbol's or a global label's name; a sheet's label under the sheet's path ({@code /power/VIN});
 *                else {@code Net-(R5-Pad1)}, after the first pin it joins
 * @param pins    the pins it joins, power symbols' own pins left out
 */
public record KiCadNet(String name, List<KiCadConnection> pins) {

    public KiCadNet {
        pins = List.copyOf(pins);
    }
}
