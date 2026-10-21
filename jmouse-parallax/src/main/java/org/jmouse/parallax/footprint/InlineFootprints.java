package org.jmouse.parallax.footprint;

import org.jmouse.datamap.DataMap;
import org.jmouse.datamap.DataMapEntry;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.FootprintOutline;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.parallax.footprint.FootprintDrafting.number;
import static org.jmouse.parallax.footprint.FootprintDrafting.point;
import static org.jmouse.parallax.footprint.SmallOutlineFootprints.rowY;

/**
 * Upright transistor outlines — TO-126, TO-247, TO-3P —: the leads in one row on the X axis, the body behind them with
 * its marked face towards −Y and its tab at the back. A pad is as tall as its ring and as wide as the pitch allows, so it
 * is an oval where neighbours narrow it. Origin at the middle of the row.
 */
final class InlineFootprints implements FootprintFamily {

    @Override
    public List<StandardFootprint> footprints(DataMap table, LandPatternRules rules) {
        List<StandardFootprint> footprints = new ArrayList<>();
        for (DataMapEntry entry : table.entries()) {
            footprints.add(footprint(new PackageTable(entry), rules));
        }
        return footprints;
    }

    private StandardFootprint footprint(PackageTable outline, LandPatternRules rules) {
        LandPatternRules.ThroughHole holes = rules.throughHole();
        LandPatternRules.Drawing drawing = rules.drawing();
        FootprintDrafting drafting = new FootprintDrafting(drawing);
        int leads = (int) outline.number("leads");
        double pitch = outline.length("pitch");
        double drill = holes.drill(outline.length("lead"), outline.length("thickness"));
        double padHeight = holes.pad(drill);
        double padWidth = holes.pad(drill, pitch);

        List<Block.FootprintPad> pads = new ArrayList<>();
        for (int index = 0; index < leads; index++) {
            pads.add(drafting.throughHolePad(String.valueOf(index + 1), rowY(index, leads, pitch), 0, padWidth, padHeight, drill));
        }
        double halfWidth = outline.length("body-width") / 2;
        double back = outline.length("back");
        double front = back - outline.length("body-depth");
        double tab = outline.length("tab");
        double offset = drawing.silkscreenOffset();
        double firstX = rowY(0, leads, pitch);
        double lastX = -firstX;
        List<FootprintOutline> outlines = new ArrayList<>();
        outlines.add(drafting.fabrication("M" + point(-halfWidth, front) + "H" + number(halfWidth) + "V" + number(back)
                + "H" + number(-halfWidth) + "Z" + (tab > 0 ? "M" + point(-halfWidth, back - tab) + "H" + number(halfWidth) : "")));
        outlines.add(drafting.silkscreen("M" + point(-halfWidth - offset, front - offset) + "H" + number(halfWidth + offset)
                + "V" + number(back + offset) + "H" + number(-halfWidth - offset) + "Z"));
        outlines.add(drafting.pinMark(firstX, Math.min(front + drawing.pinMarkRadius() + drawing.silkscreenWidth(),
                -padHeight / 2 - drawing.clearOfCopper() - drawing.pinMarkRadius())));
        double courtyard = holes.courtyard();
        outlines.add(drafting.courtyard(-Math.max(halfWidth, lastX + padWidth / 2) - courtyard,
                Math.min(front, -padHeight / 2) - courtyard, Math.max(halfWidth, lastX + padWidth / 2) + courtyard,
                Math.max(back, padHeight / 2) + courtyard));

        String slug = outline.word("name") + "-footprint";
        String title = outline.title() + ", standing";
        String summary = leads + "-lead " + outline.title() + " standing upright, through-hole, " + number(pitch)
                + " mm pitch, the tab at the back";
        List<String> notes = List.of(
                "Generated from footprint-standards/inline-packages.jmd#" + outline.id() + " and the land pattern rules; regenerate, never edit.",
                "Lead " + number(outline.length("lead")) + " × " + number(outline.length("thickness")) + " at most: drill "
                        + number(drill) + ", pad " + number(padWidth) + " × " + number(padHeight) + ", pin 1 square.");
        return new StandardFootprint(slug, title, summary, outline.word("folder"), "through-hole", outline.standard(),
                rules.license(), notes, new Block.Footprint(pads, outlines));
    }
}
