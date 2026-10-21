package org.jmouse.parallax.footprint;

import org.jmouse.datamap.DataMap;
import org.jmouse.datamap.DataMapEntry;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.FootprintOutline;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jmouse.parallax.footprint.FootprintDrafting.number;
import static org.jmouse.parallax.footprint.FootprintDrafting.point;

/**
 * Gull-wing small-outline packages — SOIC, QSOP, SSOP, TSSOP, MSOP —: two rows of leads, pin 1 at the top of the left
 * row, counted down it and up the right one.
 */
final class SmallOutlineFootprints implements FootprintFamily {

    @Override
    public List<StandardFootprint> footprints(DataMap table, LandPatternRules rules) {
        List<StandardFootprint> footprints = new ArrayList<>();
        for (DataMapEntry entry : table.entries()) {
            PackageTable outline = new PackageTable(entry);
            for (Map.Entry<Integer, Double> member : outline.members("members").entrySet()) {
                footprints.add(footprint(outline, member.getKey(), member.getValue(), rules));
            }
        }
        return footprints;
    }

    private StandardFootprint footprint(PackageTable outline, int leads, double bodyLength, LandPatternRules rules) {
        LandPatternRules.Fillets fillets = rules.gullWing();
        LandPatternRules.Drawing drawing = rules.drawing();
        FootprintDrafting drafting = new FootprintDrafting(drawing);
        double pitch = outline.length("pitch");
        PackageTable.Span span = outline.span("span");
        PackageTable.Span foot = outline.span("foot");
        PackageTable.Span lead = outline.span("lead");
        double bodyWidth = outline.length("body-width");

        double outer = span.maximum() + 2 * fillets.toe();
        double inner = span.minimum() - 2 * foot.maximum() - 2 * fillets.heel();
        double padLength = drawing.nearest((outer - inner) / 2);
        double centre = drawing.nearest((outer + inner) / 4);
        double padWidth = drawing.up(lead.maximum() + 2 * fillets.side());

        int perSide = leads / 2;
        List<Block.FootprintPad> pads = new ArrayList<>();
        for (int index = 0; index < perSide; index++) {
            pads.add(drafting.surfacePad(String.valueOf(index + 1), -centre, rowY(index, perSide, pitch), padLength, padWidth));
        }
        for (int index = 0; index < perSide; index++) {
            pads.add(drafting.surfacePad(String.valueOf(perSide + index + 1), centre,
                    rowY(perSide - 1 - index, perSide, pitch), padLength, padWidth));
        }

        double halfWidth = bodyWidth / 2;
        double halfLength = bodyLength / 2;
        double padOuter = centre + padLength / 2;
        double lineY = halfLength + drawing.silkscreenOffset();
        double lastPadEdge = rowY(perSide - 1, perSide, pitch) + padWidth / 2;
        List<FootprintOutline> outlines = List.of(
                drafting.body(bodyWidth, bodyLength),
                drafting.silkscreen("M" + point(-padOuter, -lineY) + "H" + number(halfWidth)
                        + "M" + point(-halfWidth, lineY) + "H" + number(halfWidth)),
                drafting.courtyard(Math.max(padOuter, halfWidth) + fillets.courtyard(),
                        Math.max(halfLength, lastPadEdge) + fillets.courtyard()));

        String name = outline.word("name");
        String suffix = outline.optionalWord("suffix").map(word -> "-" + word).orElse("");
        String slug = name + "-" + leads + suffix + "-footprint";
        String title = name.toUpperCase() + "-" + leads + ", " + number(bodyWidth) + " mm body";
        String summary = leads + "-lead " + outline.title() + ", gull-wing, " + number(pitch) + " mm pitch, body "
                + number(bodyWidth) + " × " + number(bodyLength) + " mm";
        List<String> notes = List.of(
                "Generated from footprint-standards/small-outline-packages.jmd#" + outline.id() + " and the land pattern rules; regenerate, never edit.",
                "Leads span " + number(span.minimum()) + "…" + number(span.maximum()) + ", foot " + number(foot.minimum()) + "…"
                        + number(foot.maximum()) + ", width " + number(lead.minimum()) + "…" + number(lead.maximum())
                        + "; fillets toe " + number(fillets.toe()) + ", heel " + number(fillets.heel()) + ", side " + number(fillets.side()) + ".",
                "Z = " + number(outer) + ", G = " + number(inner) + ", X = " + number(lead.maximum() + 2 * fillets.side())
                        + "; pad " + number(padLength) + " × " + number(padWidth) + " centred at ±" + number(centre) + ".");
        return new StandardFootprint(slug, title, summary, outline.word("folder"), "surface", outline.standard(),
                rules.license(), notes, new Block.Footprint(pads, outlines));
    }

    /** The n-th lead of a row, the row centred on the origin. */
    static double rowY(int index, int count, double pitch) {
        return (index - (count - 1) / 2.0) * pitch;
    }
}
