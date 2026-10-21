package org.jmouse.parallax.symbol;

import org.jmouse.parallax.footprint.PackageTable;
import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Connector symbols drawn from their contact count: a single-row header, a double-row one numbered as a ribbon cable
 * counts, a screw terminal block. Contacts stand on the 40 pitch, the first at −40 × ⌊(n − 1) / 2⌋ so every contact
 * lands on the sheet's grid and the drawing stays near its origin.
 */
final class ConnectorSymbols {

    /** The pitch every contact stands on: KiCad's 2.54 mm, in sheet units. */
    private static final int PITCH = 40;

    /** A symbol of the library, written as a jMX document. */
    record GeneratedSymbol(String slug, String folder, String document) {
    }

    List<GeneratedSymbol> symbols(Sheet table) {
        String license = table.field("license").map(field -> field.value().written().replace("\"", "")).orElseThrow(() ->
                new IllegalArgumentException("The connector symbols name no licence"));
        List<GeneratedSymbol> symbols = new ArrayList<>();
        for (SheetEntry entry : table.entries()) {
            PackageTable family = new PackageTable(entry);
            for (int contacts : family.counts("pins")) {
                String slug = PackageTable.named(family.word("name"), Map.of("pins", String.valueOf(contacts))) + "-symbol";
                String document = switch (family.word("style")) {
                    case "single-row" -> singleRow(slug, family.title(), contacts, license);
                    case "double-row" -> doubleRow(slug, family.title(), contacts, license);
                    case "screw" -> screw(slug, family.title(), contacts, license);
                    default -> throw new IllegalArgumentException("No connector style '" + family.word("style") + "'");
                };
                symbols.add(new GeneratedSymbol(slug, family.word("folder"), document));
            }
        }
        return symbols;
    }

    private static String singleRow(String slug, String title, int contacts, String license) {
        int first = first(contacts);
        StringBuilder path = new StringBuilder();
        StringBuilder pins = new StringBuilder();
        for (int index = 0; index < contacts; index++) {
            int y = first + index * PITCH;
            path.append("M-4 ").append(y).append("H40M-8 ").append(y - 2).append("H-4V").append(y + 2).append("H-8Z");
            pins.append(pin(index + 1, 40, y));
        }
        int last = first + (contacts - 1) * PITCH;
        return head(title + ", " + contacts + (contacts == 1 ? " contact" : " contacts"), slug,
                "A single-row pin header of " + contacts + ", as drawn on a schematic: each contact a pin leaving to the right, numbered as on the board",
                license)
                + "    size 60 x " + height(contacts) + "\n"
                + "    path \"" + path + "\"\n"
                + pins
                + "    label name at -4, " + (first - 16) + "\n"
                + "    label value at -4, " + (last + 20) + "\n"
                + "}\n";
    }

    private static String doubleRow(String slug, String title, int rows, String license) {
        int first = first(rows);
        int last = first + (rows - 1) * PITCH;
        StringBuilder path = new StringBuilder("M-20 " + (first - 10) + "H20V" + (last + 10) + "H-20Z");
        StringBuilder pins = new StringBuilder();
        for (int index = 0; index < rows; index++) {
            int y = first + index * PITCH;
            path.append("M-60 ").append(y).append("H-20M20 ").append(y).append("H60");
            pins.append(pin(2 * index + 1, -60, y)).append(pin(2 * index + 2, 60, y));
        }
        return head(title + ", 2 x " + rows, slug,
                "A two-row pin header of " + 2 * rows + ", as drawn on a schematic: odd contacts on the left, even on the right, numbered as on the board",
                license)
                + "    size 120 x " + height(rows) + "\n"
                + "    path \"" + path + "\"\n"
                + pins
                + "    label name at 0, " + (first - 24) + "\n"
                + "    label value at 0, " + (last + 28) + "\n"
                + "}\n";
    }

    private static String screw(String slug, String title, int contacts, String license) {
        int first = first(contacts);
        int last = first + (contacts - 1) * PITCH;
        StringBuilder path = new StringBuilder("M-20 " + (first - 10) + "H20V" + (last + 10) + "H-20Z");
        StringBuilder pins = new StringBuilder();
        for (int index = 0; index < contacts; index++) {
            int y = first + index * PITCH;
            path.append("M-40 ").append(y).append("H-6M-6 ").append(y).append("A6 6 0 1 0 6 ").append(y)
                    .append("A6 6 0 1 0 -6 ").append(y).append("ZM-4 ").append(y + 4).append("L4 ").append(y - 4);
            pins.append(pin(index + 1, -40, y));
        }
        return head(title + ", " + contacts + (contacts == 1 ? " contact" : " contacts"), slug,
                "A terminal block of " + contacts + " screw clamps, as drawn on a schematic: each wire comes in from the left under its screw",
                license)
                + "    size 80 x " + height(contacts) + "\n"
                + "    path \"" + path + "\"\n"
                + pins
                + "    label name at 0, " + (first - 24) + "\n"
                + "    label value at 0, " + (last + 28) + "\n"
                + "}\n";
    }

    private static String head(String title, String slug, String summary, String license) {
        return "symbol \"" + title + "\" as " + slug + " {\n"
                + "    meta {\n"
                + "        summary \"" + summary + "\"\n"
                + "        category circuits\n"
                + "        tags electronics, schematic, connector\n"
                + "        license \"" + license + "\"\n"
                + "    }\n"
                + "    # Generated from symbol-standards/connector-symbols.sht; regenerate, never edit.\n";
    }

    private static String pin(int number, int x, int y) {
        return "    pin pin" + number + " at " + x + ", " + y + " \"" + number + "\"\n";
    }

    private static int first(int contacts) {
        return -PITCH * ((contacts - 1) / 2);
    }

    private static int height(int contacts) {
        return PITCH * (contacts - 1) + 60;
    }
}
