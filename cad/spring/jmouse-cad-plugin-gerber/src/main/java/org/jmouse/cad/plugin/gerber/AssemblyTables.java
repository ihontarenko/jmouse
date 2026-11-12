package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.exchange.pcb.BillOfMaterials;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.exchange.ExportedFile;
import org.jmouse.core.text.CsvText;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The assembly tables of a pcb, written by a profile: where each seated part stands — the placement, pick-and-place —
 * and what to buy — the bill of materials, one line a kind of part, a kind being its component, value and footprint.
 * Millimetres, Y up as an assembler's machine reads it, degrees counter-clockwise; CSV, a field quoted when it must be.
 */
public final class AssemblyTables {

    /** The prefix of a field read from the component's own head: {@code meta:lcsc}. */
    private static final String META = "meta:";

    private AssemblyTables() {
    }

    /** Both tables, named after the set. */
    public static List<ExportedFile> of(PcbFabrication fabrication, AssemblyProfile profile) {
        return List.of(
                new ExportedFile(fabrication.name() + "-" + profile.placement().file(), placement(fabrication, profile.placement())),
                new ExportedFile(fabrication.name() + "-" + profile.materials().file(), materials(fabrication, profile.materials())));
    }

    /** Where each seated part stands. */
    public static String placement(PcbFabrication fabrication, AssemblyProfile.PlacementTable table) {
        List<String> lines = new ArrayList<>();
        lines.add(headings(table));
        int item = 0;
        for (PcbFabrication.SeatedPart part : fabrication.parts()) {
            // Nothing a machine places.
            if (part.nothingToMount()) {
                continue;
            }
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("item", String.valueOf(++item));
            fields.put("mounting", mounting(part, table));
            fields.put("designator", uniquelyNamed(part, fabrication));
            fields.put("value", part.value());
            fields.put("footprint", part.footprint().orElse(""));
            fields.put("component", part.component());
            fields.put("x", length(part.placement().x(), table));
            fields.put("y", length(-part.placement().y(), table));
            fields.put("rotation", plain(((part.placement().rotation() % 360) + 360) % 360));
            fields.put("side", table.sides().getOrDefault(part.placement().side(), part.placement().side()));
            lines.add(row(table, fields, fabrication, part.component()));
        }
        return String.join("\n", lines) + "\n";
    }

    /**
     * What a placement table calls a part, which a machine goes by, so each name must be one part's: what it prints, unless
     * another part prints the same — the copies of a board on a panel —, and then its own name on the pcb.
     */
    private static String uniquelyNamed(PcbFabrication.SeatedPart part, PcbFabrication fabrication) {
        long printedAlike = fabrication.parts().stream().filter(other -> other.printed().equals(part.printed())).count();
        return printedAlike > 1 ? part.designator() : part.printed();
    }

    /** What to buy: the pcb's bill of materials, a line a kind of part, a part with no footprint listed too. */
    public static String materials(PcbFabrication fabrication, AssemblyProfile.MaterialsTable table) {
        List<String> lines = new ArrayList<>();
        lines.add(headings(table));
        int item = 0;
        for (BillOfMaterials.Line kind : BillOfMaterials.of(fabrication).lines()) {
            PcbFabrication.SeatedPart first = kind.parts().getFirst();
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("item", String.valueOf(++item));
            fields.put("mounting", mounting(first, table));
            fields.put("designators", String.join(table.designatorSeparator(), kind.designators()));
            fields.put("designator", first.printed());
            fields.put("quantity", String.valueOf(kind.quantity()));
            fields.put("value", kind.value());
            fields.put("footprint", kind.footprint().orElse(""));
            fields.put("component", kind.component());
            lines.add(row(table, fields, fabrication, kind.component()));
        }
        return String.join("\n", lines) + "\n";
    }

    /** How a part is mounted — through a hole or on the surface —, as the table says it. */
    private static String mounting(PcbFabrication.SeatedPart part, AssemblyProfile.Table table) {
        String way = part.throughHole() ? "through-hole" : "surface";
        return table.mountings().getOrDefault(way, way);
    }

    private static String headings(AssemblyProfile.Table table) {
        return CsvText.row(table.columns().stream().map(AssemblyProfile.Column::heading).toList());
    }

    private static String row(AssemblyProfile.Table table, Map<String, String> fields, PcbFabrication fabrication,
                              String component) {
        List<String> cells = new ArrayList<>();
        for (AssemblyProfile.Column column : table.columns()) {
            String value;
            if (column.field().startsWith(META)) {
                value = meta(fabrication, component, column.field().substring(META.length())).orElse("");
            } else if (fields.containsKey(column.field())) {
                value = fields.get(column.field());
            } else {
                throw new IllegalStateException("The profile's column '" + column.heading() + "' shows '" + column.field()
                        + "', which no table has.");
            }
            cells.add(value);
        }
        return CsvText.row(cells);
    }

    /** A line of the component's own head: {@code meta lcsc C25804} for {@code lcsc}. */
    private static Optional<String> meta(PcbFabrication fabrication, String component, String name) {
        return fabrication.facts().component(component).flatMap(written -> written.head().field(name));
    }

    private static String length(double millimetres, AssemblyProfile.PlacementTable table) {
        return plain(millimetres) + table.unit();
    }

    /** A number as the files write it: four places, no trailing zeros, never {@code -0}. */
    private static String plain(double value) {
        BigDecimal rounded = BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
        if (rounded.signum() == 0) {
            return "0";
        }
        return rounded.toPlainString();
    }
}
