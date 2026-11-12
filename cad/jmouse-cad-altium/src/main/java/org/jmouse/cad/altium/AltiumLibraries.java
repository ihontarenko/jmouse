package org.jmouse.cad.altium;

import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.exchange.imported.FilingTable;
import org.jmouse.cad.exchange.imported.ImportFiling;
import org.jmouse.cad.exchange.imported.SheetFiling;

import java.util.Map;
import java.util.Optional;

/**
 * How documents converted from Altium are filed, as {@code altium-libraries.sht} beside this class writes it: what every
 * table says, the net that is node 0, and where a PCB library's layers land among ours — a fixed layer by Altium's
 * number, a mechanical layer by the kind its library names for it. An Altium sheet carries each part's drawing: what is
 * converted from it is the design's, so it names no holder and no terms.
 *
 * @param sheet           what it says as every product's table does: category, tags, transient, mark symbols
 * @param ground          the net that is node 0
 * @param layers          our layer each fixed Altium layer is, by Altium's number as text
 * @param mechanicalKinds our layer each kind of mechanical layer is, by Altium's number of the kind as text
 * @param multiLayer      the layer a pad through the board stands on
 * @param bottomLayer     the copper layer of the far side
 */
public record AltiumLibraries(SheetFiling sheet, String ground, Map<String, String> layers, Map<String, String> mechanicalKinds,
                              int multiLayer, int bottomLayer) implements ImportFiling {

    private static final String RESOURCE = "altium-libraries.sht";

    public AltiumLibraries {
        layers = Map.copyOf(layers);
        mechanicalKinds = Map.copyOf(mechanicalKinds);
    }

    /** The table this library ships. */
    public static AltiumLibraries standard() {
        return of(FilingTable.shipped(AltiumLibraries.class, RESOURCE));
    }

    public static AltiumLibraries of(FilingTable table) {
        return new AltiumLibraries(SheetFiling.of(table), table.text("ground"), table.layers("altium"), table.layers("mechanical-kind"),
                Integer.parseInt(table.text("multi-layer")), Integer.parseInt(table.text("bottom-layer")));
    }

    @Override
    public Optional<String> layerOf(String layer) {
        return Optional.ofNullable(layers.get(layer));
    }

    /**
     * Our layer an Altium layer is in one library: a fixed one by its number, a mechanical one by the kind the library
     * names for it ({@code kinds}, Altium's numbers). Empty for a layer neither says.
     */
    public Optional<String> layerOf(int layer, Map<Integer, Integer> kinds) {
        return layerOf(String.valueOf(layer))
                .or(() -> Optional.ofNullable(kinds.get(layer)).map(kind -> mechanicalKinds.get(String.valueOf(kind))));
    }

    /** The Altium library a converted drawing comes from, as its source says it ({@link AltiumNames#libraryOf}). */
    @Override
    public Optional<String> libraryOf(Provenance provenance) {
        return AltiumNames.libraryOf(provenance.source());
    }

    /** A converted drawing is the design's: its source said, no holder and no terms claimed. */
    @Override
    public Provenance provenance(String source, boolean installed) {
        return new Provenance(source, null, null);
    }
}
