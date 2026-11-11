package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.exchange.ExportedFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The assembly tables of a board, written by a profile: where each seated part stands — the placement, pick-and-place —
 * and what to buy — the bill of materials, one line a kind of part, a kind being its component, value and footprint.
 * Millimetres, Y up as an assembler's machine reads it, degrees counter-clockwise; CSV, a field quoted when it must be.
 */
public final class AssemblyTables {

    /** The prefix of a field read from the component's own head: {@code meta:lcsc}. */
    private static final String META = "meta:";

    /** What makes a field need quotes: a comma, a quote, a line break. */
    private static final Pattern QUOTED = Pattern.compile("[\",\\n]");

    private AssemblyTables() {
    }

    /** Both tables, named after the set. */
    public static List<ExportedFile> of(BoardFabrication fabrication, AssemblyProfile profile) {
        return List.of(
                new ExportedFile(fabrication.name() + "-" + profile.placement().file(), placement(fabrication, profile.placement())),
                new ExportedFile(fabrication.name() + "-" + profile.materials().file(), materials(fabrication, profile.materials())));
    }

    /** Where each seated part stands. */
    public static String placement(BoardFabrication fabrication, AssemblyProfile.Table table) {
        List<String> lines = new ArrayList<>();
        lines.add(headings(table));
        int item = 0;
        for (BoardFabrication.SeatedPart part : fabrication.parts()) {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("item", String.valueOf(++item));
            fields.put("mounting", mounting(part, table));
            fields.put("designator", part.designator());
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

    /** What to buy: the seated parts grouped by component, value and footprint, a part with no footprint listed too. */
    public static String materials(BoardFabrication fabrication, AssemblyProfile.Table table) {
        Map<String, List<BoardFabrication.SeatedPart>> kinds = new LinkedHashMap<>();
        for (BoardFabrication.SeatedPart part : fabrication.parts()) {
            String kind = part.component() + "\u0000" + part.value() + "\u0000" + part.footprint().orElse("");
            kinds.computeIfAbsent(kind, ignored -> new ArrayList<>()).add(part);
        }
        List<String> lines = new ArrayList<>();
        lines.add(headings(table));
        int item = 0;
        for (List<BoardFabrication.SeatedPart> kind : kinds.values()) {
            BoardFabrication.SeatedPart first = kind.getFirst();
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("item", String.valueOf(++item));
            fields.put("mounting", mounting(first, table));
            fields.put("designators", String.join(table.designatorSeparator(),
                    kind.stream().map(BoardFabrication.SeatedPart::designator).toList()));
            fields.put("designator", first.designator());
            fields.put("quantity", String.valueOf(kind.size()));
            fields.put("value", first.value());
            fields.put("footprint", first.footprint().orElse(""));
            fields.put("component", first.component());
            lines.add(row(table, fields, fabrication, first.component()));
        }
        return String.join("\n", lines) + "\n";
    }

    /** How a part is mounted, by its pads — any through a hole makes it {@code through-hole} —, as the table says it. */
    private static String mounting(BoardFabrication.SeatedPart part, AssemblyProfile.Table table) {
        boolean throughHole = part.drawing().stream().flatMap(drawing -> drawing.pads().stream())
                .anyMatch(pad -> "through-hole".equals(pad.type()));
        String way = throughHole ? "through-hole" : "surface";
        return table.mountings().getOrDefault(way, way);
    }

    private static String headings(AssemblyProfile.Table table) {
        return String.join(",", table.columns().stream().map(column -> field(column.heading())).toList());
    }

    private static String row(AssemblyProfile.Table table, Map<String, String> fields, BoardFabrication fabrication,
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
            cells.add(field(value));
        }
        return String.join(",", cells);
    }

    /** A line of the component's own head: {@code meta lcsc C25804} for {@code lcsc}. */
    private static Optional<String> meta(BoardFabrication fabrication, String component, String name) {
        return fabrication.facts().component(component).flatMap(written -> written.head().field(name));
    }

    private static String length(double millimetres, AssemblyProfile.Table table) {
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

    private static String field(String text) {
        return QUOTED.matcher(text).find() ? "\"" + text.replace("\"", "\"\"") + "\"" : text;
    }
}
