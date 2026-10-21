package org.jmouse.parallax.footprint;

import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.FootprintOutline;
import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.parallax.footprint.FootprintDrafting.number;
import static org.jmouse.parallax.footprint.FootprintDrafting.point;
import static org.jmouse.parallax.footprint.SmallOutlineFootprints.rowY;

/**
 * Square packages with leads on all four sides — gull-wing QFPs and bottom-terminated QFNs —: pin 1 at the top of the
 * left side, counted down it, along the bottom, up the right side and back along the top, as every maker counts them.
 * A thermal pad under the body is the number after the last lead. The silkscreen marks the body's corners and pin 1.
 */
final class QuadFootprints implements FootprintFamily {

    @Override
    public List<StandardFootprint> footprints(Sheet table, LandPatternRules rules) {
        List<StandardFootprint> footprints = new ArrayList<>();
        for (SheetEntry entry : table.entries()) {
            footprints.add(footprint(new PackageTable(entry), rules));
        }
        return footprints;
    }

    private StandardFootprint footprint(PackageTable outline, LandPatternRules rules) {
        LandPatternRules.Drawing drawing = rules.drawing();
        LandPatternRules.Fillets fillets = rules.fillets(outline.word("fillets"));
        FootprintDrafting drafting = new FootprintDrafting(drawing);
        int leads = (int) outline.number("leads");
        int perSide = leads / 4;
        double pitch = outline.length("pitch");
        double body = outline.length("body");
        SurfaceLand land = SurfaceLand.of(outline.span("span"), outline.span("foot"), outline.span("lead"), fillets,
                rules.tolerances());
        double padLength = land.padLength(drawing);
        double centre = land.centre(drawing);
        double padWidth = land.padWidth(drawing, rules.tolerances(), pitch);

        List<Block.FootprintPad> pads = new ArrayList<>();
        for (int index = 0; index < perSide; index++) {
            double along = rowY(index, perSide, pitch);
            pads.add(drafting.surfacePad(String.valueOf(index + 1), -centre, along, padLength, padWidth));
            pads.add(drafting.surfacePad(String.valueOf(perSide + index + 1), along, centre, padWidth, padLength));
            pads.add(drafting.surfacePad(String.valueOf(2 * perSide + index + 1), centre, -along, padLength, padWidth));
            pads.add(drafting.surfacePad(String.valueOf(3 * perSide + index + 1), -along, -centre, padWidth, padLength));
        }
        pads.sort((left, right) -> Integer.compare(Integer.parseInt(left.number()), Integer.parseInt(right.number())));
        if (outline.has("exposed-pad")) {
            double exposed = outline.length("exposed-pad");
            pads.add(drafting.surfacePad(String.valueOf(leads + 1), 0, 0, exposed, exposed));
        }

        // A corner mark runs from the corner to just short of the first pad on each side.
        double half = body / 2;
        double corner = half + drawing.silkscreenOffset();
        double firstPad = Math.abs(rowY(0, perSide, pitch)) + padWidth / 2 + drawing.clearOfCopper();
        StringBuilder marks = new StringBuilder();
        if (corner > firstPad) {
            for (int horizontal = -1; horizontal <= 1; horizontal += 2) {
                for (int vertical = -1; vertical <= 1; vertical += 2) {
                    marks.append("M").append(point(horizontal * firstPad, vertical * corner))
                            .append("H").append(number(horizontal * corner))
                            .append("V").append(number(vertical * firstPad));
                }
            }
        }
        double padOuter = centre + padLength / 2;
        List<FootprintOutline> outlines = new ArrayList<>();
        outlines.add(drafting.body(body, body));
        if (!marks.isEmpty()) {
            outlines.add(drafting.silkscreen(marks.toString()));
        }
        outlines.add(drafting.pinMark(-padOuter - drawing.clearOfCopper() - drawing.pinMarkRadius(), rowY(0, perSide, pitch)));
        double reach = Math.max(padOuter, half) + fillets.courtyard();
        outlines.add(drafting.courtyard(reach, reach));

        String slug = outline.word("name") + "-footprint";
        String title = outline.title();
        String summary = leads + "-lead " + outline.title() + ", " + number(pitch) + " mm pitch, body " + number(body)
                + " × " + number(body) + " mm" + (outline.has("exposed-pad") ? ", exposed pad " + number(outline.length("exposed-pad")) + " mm" : "");
        List<String> notes = List.of(
                "Generated from footprint-standards/quad-packages.sht#" + outline.id() + " and the land pattern rules; regenerate, never edit.",
                "Z = " + number(land.outer()) + ", G = " + number(land.inner()) + ", X = " + number(land.width())
                        + "; pad " + number(padLength) + " × " + number(padWidth) + " centred at ±" + number(centre) + ".");
        return new StandardFootprint(slug, title, summary, outline.word("folder"), "surface", outline.standard(),
                rules.license(), notes, new Block.Footprint(pads, outlines));
    }
}
