package org.jmouse.cad.packages.footprint;

import org.jmouse.cad.packages.PackageTable;
import org.jmouse.cad.packages.MeasuredPath;
import org.jmouse.cad.packages.footprint.FootprintDrawing.Outline;
import org.jmouse.cad.packages.footprint.FootprintDrawing.Pad;
import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.jmouse.cad.packages.footprint.FootprintDrafting.rowY;
import static org.jmouse.cad.packages.Measures.written;

/**
 * Pin headers and sockets standing upright: rows of square leads on the pitch, numbered across the rows — 1 and 2 side
 * by side, then 3 and 4 —, as a ribbon cable counts them.
 */
final class HeaderFootprints implements FootprintFamily {

    @Override
    public String form() {
        return "header";
    }

    @Override
    public List<StandardFootprint> footprints(Sheet table, LandPatternRules rules) {
        List<StandardFootprint> footprints = new ArrayList<>();
        for (SheetEntry entry : table.entries()) {
            PackageTable header = new PackageTable(entry);
            for (int rows : header.counts("rows")) {
                for (int pins : header.counts("pins")) {
                    footprints.add(footprint(header, rows, pins, rules));
                }
            }
        }
        return footprints;
    }

    private StandardFootprint footprint(PackageTable header, int rows, int pinsPerRow, LandPatternRules rules) {
        LandPatternRules.ThroughHole holes = rules.throughHole();
        LandPatternRules.Drawing drawing = rules.drawing();
        FootprintDrafting drafting = new FootprintDrafting(drawing);
        double pitch = header.length("pitch");
        double drill = holes.drill(header.length("lead"), header.length("thickness"));
        double pad = holes.pad(drill, pitch);
        List<Pad> pads = new ArrayList<>();
        int number = 1;
        for (int index = 0; index < pinsPerRow; index++) {
            for (int column = 0; column < rows; column++) {
                pads.add(drafting.throughHolePad(String.valueOf(number), rowY(column, rows, pitch),
                        rowY(index, pinsPerRow, pitch), pad, drill));
                number += 1;
            }
        }
        // A terminal block's body is deeper than its pitch: the screw and the wire's mouth stand behind the row.
        double bodyWidth = header.has("body-depth") ? header.length("body-depth") : rows * pitch;
        double bodyLength = pinsPerRow * pitch;
        double silkHalfWidth = bodyWidth / 2 + drawing.silkscreenOffset();
        double silkHalfLength = bodyLength / 2 + drawing.silkscreenOffset();
        double markX = -silkHalfWidth - drawing.silkscreenClearance() - drawing.pinMarkRadius();
        List<Outline> outlines = List.of(
                drafting.body(bodyWidth, bodyLength),
                drafting.silkscreen(MeasuredPath.path().rectangle(silkHalfWidth, silkHalfLength)),
                drafting.pinMark(markX, rowY(0, pinsPerRow, pitch)),
                drafting.courtyard(Math.max(bodyWidth, (rows - 1) * pitch + pad) / 2 + holes.courtyard(),
                        Math.max(bodyLength, (pinsPerRow - 1) * pitch + pad) / 2 + holes.courtyard()));
        String name = header.word("name");
        String pitchWord = PackageTable.hundredths(pitch);
        String standing = header.optionalWord("standing").orElse("vertical");
        String slug = name + "-" + rows + "x" + pinsPerRow + "-pitch-" + pitchWord + "-" + standing + "-footprint";
        Map<String, String> words = new LinkedHashMap<>(header.words());
        words.put("rows", String.valueOf(rows));
        words.put("pins", String.valueOf(pinsPerRow));
        words.put("standing", standing);
        String designated = PackageTable.designated(header.word("designation"), words);
        String title = header.title() + ", " + rows + " × " + pinsPerRow + ", " + standing;
        String summary = header.title() + ", " + rows + (rows == 1 ? " row" : " rows") + " of " + pinsPerRow
                + ", upright, through-hole";
        List<String> notes = List.of(
                "Generated from footprint-standards/header-packages.sht#" + header.id() + " and the land pattern rules; regenerate, never edit.",
                "Square lead " + written(header.length("lead")) + ": drill " + written(drill) + ", pad " + written(pad) + ", pin 1 square.");
        return new StandardFootprint(slug, designated, title, summary, header.word("folder"), "through-hole", header.standard(),
                rules, notes, new FootprintDrawing(pads, outlines));
    }
}
