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
 * Dual in-line packages: two rows of holes, pin 1 at the top of the left row, counted down it and up the right one; the
 * silkscreen outlines the body between the rows, its first end notched.
 */
final class DualInlineFootprints implements FootprintFamily {

    @Override
    public String form() {
        return "dual-inline";
    }

    @Override
    public List<StandardFootprint> footprints(Sheet table, LandPatternRules rules) {
        List<StandardFootprint> footprints = new ArrayList<>();
        for (SheetEntry entry : table.entries()) {
            PackageTable outline = new PackageTable(entry);
            for (Map.Entry<Integer, Double> member : outline.members("members").entrySet()) {
                footprints.add(footprint(outline, member.getKey(), member.getValue(), rules));
            }
        }
        return footprints;
    }

    private StandardFootprint footprint(PackageTable outline, int leads, double bodyLength, LandPatternRules rules) {
        LandPatternRules.ThroughHole holes = rules.throughHole();
        LandPatternRules.Drawing drawing = rules.drawing();
        FootprintDrafting drafting = new FootprintDrafting(drawing);
        double pitch = outline.length("pitch");
        double row = outline.length("row");
        double bodyWidth = outline.length("body-width");
        double drill = holes.drill(outline.length("lead"), outline.length("thickness"));
        double pad = holes.pad(drill, pitch);
        int perSide = leads / 2;
        double column = row / 2;
        List<Pad> pads = new ArrayList<>();
        for (int index = 0; index < perSide; index++) {
            pads.add(drafting.throughHolePad(String.valueOf(index + 1), -column, rowY(index, perSide, pitch), pad, drill));
        }
        for (int index = 0; index < perSide; index++) {
            pads.add(drafting.throughHolePad(String.valueOf(perSide + index + 1), column,
                    rowY(perSide - 1 - index, perSide, pitch), pad, drill));
        }
        double halfLength = bodyLength / 2;
        double lastPad = rowY(perSide - 1, perSide, pitch);
        double silkX = column - pad / 2 - drawing.silkscreenClearance() - drawing.silkscreenWidth() / 2;
        // The outline runs between the rows, clear of every pad, so only the body sets how long it is.
        double silkY = halfLength + drawing.silkscreenOffset();
        double notch = Math.min(drawing.pinNotchRadius(), silkX / 2);
        double markX = -silkX + 2 * drawing.pinMarkRadius() + drawing.silkscreenClearance() / 2;
        List<Outline> outlines = List.of(
                drafting.body(bodyWidth, bodyLength),
                drafting.silkscreen(MeasuredPath.path().moveTo(-silkX, -silkY).horizontalTo(-notch)
                        .arcTo(notch, false, false, notch, -silkY).horizontalTo(silkX).verticalTo(silkY)
                        .horizontalTo(-silkX).close()),
                drafting.pinMark(markX, rowY(0, perSide, pitch)),
                drafting.courtyard(Math.max(column + pad / 2, bodyWidth / 2) + holes.courtyard(),
                        Math.max(lastPad + pad / 2, halfLength) + holes.courtyard()));
        String name = outline.word("name");
        String suffix = outline.optionalWord("suffix").map(word -> "-" + word).orElse("");
        String slug = name + "-" + leads + suffix + "-footprint";
        Map<String, String> words = new LinkedHashMap<>(outline.words());
        words.put("leads", String.valueOf(leads));
        String designated = PackageTable.designated(outline.word("designation"), words);
        String title = name.toUpperCase() + "-" + leads + ", " + written(row) + " mm rows";
        String summary = leads + "-lead dual in-line package, through-hole, " + written(pitch) + " mm pitch, rows "
                + written(row) + " mm apart";
        List<String> notes = List.of(
                "Generated from footprint-standards/dual-inline-packages.sht#" + outline.id() + " and the land pattern rules; regenerate, never edit.",
                "Lead " + written(outline.length("lead")) + " × " + written(outline.length("thickness")) + " at most: drill "
                        + written(drill) + ", pad " + written(pad) + ", pin 1 square; body " + written(bodyWidth) + " × " + written(bodyLength) + ".");
        return new StandardFootprint(slug, designated, title, summary, outline.word("folder"), "through-hole", outline.standard(),
                rules, notes, new FootprintDrawing(pads, outlines));
    }
}
