package org.jmouse.parallax.footprint;

import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.FootprintOutline;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.parallax.footprint.FootprintDrafting.number;
import static org.jmouse.parallax.footprint.SmallOutlineFootprints.rowY;

/**
 * Pin headers and sockets standing upright: rows of square leads on the pitch, numbered across the rows — 1 and 2 side
 * by side, then 3 and 4 —, as a ribbon cable counts them.
 */
final class HeaderFootprints implements FootprintFamily {

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

        List<Block.FootprintPad> pads = new ArrayList<>();
        int number = 1;
        for (int index = 0; index < pinsPerRow; index++) {
            for (int column = 0; column < rows; column++) {
                pads.add(drafting.throughHolePad(String.valueOf(number++), rowY(column, rows, pitch),
                        rowY(index, pinsPerRow, pitch), pad, drill));
            }
        }

        double bodyWidth = rows * pitch;
        double bodyLength = pinsPerRow * pitch;
        double silkHalfWidth = bodyWidth / 2 + drawing.silkscreenOffset();
        double silkHalfLength = bodyLength / 2 + drawing.silkscreenOffset();
        double markX = -silkHalfWidth - drawing.silkscreenClearance() - drawing.pinMarkRadius();
        List<FootprintOutline> outlines = List.of(
                drafting.body(bodyWidth, bodyLength),
                drafting.silkscreen(drafting.rectangle(silkHalfWidth, silkHalfLength)),
                drafting.pinMark(markX, rowY(0, pinsPerRow, pitch)),
                drafting.courtyard(Math.max(bodyWidth, (rows - 1) * pitch + pad) / 2 + holes.courtyard(),
                        Math.max(bodyLength, (pinsPerRow - 1) * pitch + pad) / 2 + holes.courtyard()));

        String name = header.word("name");
        String pitchWord = String.valueOf(Math.round(pitch * 100));
        String slug = name + "-" + rows + "x" + pinsPerRow + "-pitch-" + pitchWord + "-vertical-footprint";
        String title = header.title() + ", " + rows + " × " + pinsPerRow + ", vertical";
        String summary = header.title() + ", " + rows + (rows == 1 ? " row" : " rows") + " of " + pinsPerRow
                + ", upright, through-hole";
        List<String> notes = List.of(
                "Generated from footprint-standards/header-packages.sht#" + header.id() + " and the land pattern rules; regenerate, never edit.",
                "Square lead " + number(header.length("lead")) + ": drill " + number(drill) + ", pad " + number(pad) + ", pin 1 square.");
        return new StandardFootprint(slug, title, summary, header.word("folder"), "through-hole", header.standard(),
                rules.license(), notes, new Block.Footprint(pads, outlines));
    }
}
