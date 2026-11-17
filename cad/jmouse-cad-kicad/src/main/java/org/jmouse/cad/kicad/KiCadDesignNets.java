package org.jmouse.cad.kicad;

import org.jmouse.cad.exchange.imported.ImportedNetNames;

import java.util.List;
import java.util.Map;

/**
 * How a KiCad design names its nets, for each of its sheets read alone: the net that is node 0, and the net each name joining across
 * the whole design is — a sheet read alone cannot see that the root wires a VCC symbol to a +3.3V one, so every sheet's hidden VCC
 * pins are on +3.3V only by being told.
 *
 * @param ground  the KiCad net that is node 0: the design's reference ground
 * @param globals    each name joining across the design → the net it is, as the whole design's connectivity names it
 * @param busAliases the design's bus aliases, each a name for a set of nets, good on every sheet whichever declares it
 */
public record KiCadDesignNets(String ground, Map<String, String> globals, Map<String, List<String>> busAliases) {

    public KiCadDesignNets {
        globals = Map.copyOf(globals);
        busAliases = Map.copyOf(busAliases);
    }

    /** A design whose global names are each their own net: a sheet read with no design around it. */
    public static KiCadDesignNets alone(String ground) {
        return new KiCadDesignNets(ground, Map.of(), Map.of());
    }

    /** A fresh naming of a sheet's nets as jMX nets, each global name the net the design makes it. */
    public ImportedNetNames names() {
        return new ImportedNetNames(ground, KiCadNetNames::local, name -> globals.getOrDefault(name, name));
    }
}
