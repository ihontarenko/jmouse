package org.jmouse.cad.eagle;

import org.jmouse.cad.table.ReadTable;
import org.jmouse.cad.table.TableSource;
import org.jmouse.cad.table.ShippedTables;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.exchange.imported.FilingTable;
import org.jmouse.cad.exchange.imported.ImportFiling;
import org.jmouse.cad.exchange.imported.SheetFiling;

import java.util.Map;
import java.util.Optional;

/**
 * How documents converted from an Eagle design are filed, as {@code eagle-libraries.sht} beside this class writes it:
 * the category and tags, the marks a sheet is drawn with, the net that is node 0, which of our layers each Eagle layer is, and the ring Eagle's
 * design rules give a pad that leaves its own to them. An Eagle design carries its libraries: what is converted from them
 * is the design's, so it names no holder and no terms.
 *
 * @param sheet     what it says as every product's table does: category, tags, transient, mark symbols
 * @param ground    the net that is node 0
 * @param sheetGap  how far apart its sheets are laid, millimetres
 * @param ringShare the share of a drill its ring is, where the rules give it
 * @param ringLeast the least ring, millimetres
 * @param ringMost  the most, millimetres
 * @param layers    our layer each Eagle layer is, by Eagle's number as text
 */
public record EagleLibraries(SheetFiling sheet, String ground, double sheetGap, double ringShare, double ringLeast,
                             double ringMost, Map<String, String> layers) implements ImportFiling {

    /** The table's address in the catalogue. */
    public static final String TABLE = "eagle-libraries";

    /** Where its module ships it. */
    public static final ShippedTables SHIPPED = ShippedTables.in(EagleLibraries.class, "jmouse-cad-eagle", "org/jmouse/cad/eagle/tables");

    public EagleLibraries {
        layers = Map.copyOf(layers);
    }

    /** The table this library ships. */
    public static EagleLibraries shipped() {
        return readFrom(SHIPPED).current();
    }

    /**
     * As its source answers at each use — the engine's catalogue table, its curator's revision where one is kept —, read again
     * only when another revision answers.
     */
    public static ReadTable<EagleLibraries> readFrom(TableSource tables) {
        return new ReadTable<>(tables, TABLE, sheet -> of(FilingTable.of(TABLE, sheet)));
    }

    public static EagleLibraries of(FilingTable table) {
        return new EagleLibraries(SheetFiling.of(table), table.text("ground"), table.measure("sheet-gap") * 1000, Double.parseDouble(table.text("ring-share")),
                table.measure("ring-least") * 1000, table.measure("ring-most") * 1000, table.ours("eagle"));
    }

    @Override
    public Optional<String> layerOf(String layer) {
        return Optional.ofNullable(layers.get(layer));
    }

    /** The Eagle library a converted drawing comes from, as its source says it ({@link EagleNames#libraryOf}). */
    @Override
    public Optional<String> libraryOf(Provenance provenance) {
        return EagleNames.libraryOf(provenance.source());
    }

    /** A converted drawing is the design's: its source said, no holder and no terms claimed. */
    @Override
    public Provenance provenance(String source, boolean installed) {
        return new Provenance(source, null, null);
    }

    /** The diameter of a pad or a via that leaves its ring to the design rules: its drill and the rules' ring round it. */
    public double ringedDiameter(double drill) {
        return drill + 2 * Math.min(ringMost, Math.max(ringLeast, drill * ringShare));
    }
}
