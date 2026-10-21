package org.jmouse.parallax.footprint;

import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;
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
    public List<StandardFootprint> footprints(Sheet table, LandPatternRules rules) {
        List<StandardFootprint> footprints = new ArrayList<>();
        for (SheetEntry entry : table.entries()) {
            PackageTable outline = new PackageTable(entry);
            if (entry.value("left").isPresent()) {
                footprints.add(footprint(outline, outline.length("body-length"), laidOut(outline),
                        outline.word("name"), outline.word("name").toUpperCase(), rules));
                continue;
            }
            for (Map.Entry<Integer, Double> member : outline.members("members").entrySet()) {
                int leads = member.getKey();
                String suffix = outline.optionalWord("suffix").map(word -> "-" + word).orElse("");
                footprints.add(footprint(outline, member.getValue(), counted(leads, outline.length("pitch")),
                        outline.word("name") + "-" + leads + suffix, outline.word("name").toUpperCase() + "-" + leads, rules));
            }
        }
        return footprints;
    }

    /** A lead: its number, the row it stands in ({@code -1} left, {@code 1} right) and where along it. */
    record Lead(String number, int side, double y) {
    }

    /** Leads counted down the left row and up the right one, as a SOIC counts them. */
    private static List<Lead> counted(int leads, double pitch) {
        int perSide = leads / 2;
        List<Lead> laid = new ArrayList<>();
        for (int index = 0; index < perSide; index++) {
            laid.add(new Lead(String.valueOf(index + 1), -1, rowY(index, perSide, pitch)));
        }
        for (int index = 0; index < perSide; index++) {
            laid.add(new Lead(String.valueOf(perSide + index + 1), 1, rowY(perSide - 1 - index, perSide, pitch)));
        }
        return laid;
    }

    /** Leads where the table puts them, {@code left 1 = -1, 2 = 1} in pitches from the middle: a SOT-23 skips a place. */
    private static List<Lead> laidOut(PackageTable outline) {
        double pitch = outline.length("pitch");
        List<Lead> laid = new ArrayList<>();
        outline.places("left").forEach((number, place) -> laid.add(new Lead(number, -1, place * pitch)));
        outline.places("right").forEach((number, place) -> laid.add(new Lead(number, 1, place * pitch)));
        return laid;
    }

    private StandardFootprint footprint(PackageTable outline, double bodyLength, List<Lead> leads, String name,
                                        String designation, LandPatternRules rules) {
        LandPatternRules.Fillets fillets = rules.fillets("gull-wing");
        LandPatternRules.Drawing drawing = rules.drawing();
        FootprintDrafting drafting = new FootprintDrafting(drawing);
        double pitch = outline.length("pitch");
        PackageTable.Span span = outline.span("span");
        PackageTable.Span foot = outline.span("foot");
        PackageTable.Span lead = outline.span("lead");
        double bodyWidth = outline.length("body-width");

        SurfaceLand land = SurfaceLand.of(span, foot, lead, fillets, rules.tolerances());
        double padLength = land.padLength(drawing);
        double centre = land.centre(drawing);
        double padWidth = land.padWidth(drawing, rules.tolerances(), pitch);

        List<Block.FootprintPad> pads = new ArrayList<>();
        double lastPadEdge = 0;
        double firstY = leads.getFirst().y();
        for (Lead placed : leads) {
            pads.add(drafting.surfacePad(placed.number(), placed.side() * centre, placed.y(), padLength, padWidth));
            lastPadEdge = Math.max(lastPadEdge, Math.abs(placed.y()) + padWidth / 2);
        }
        // A thermal pad under the body is the number after the last lead, as a maker's pinout counts it.
        if (outline.has("exposed-pad-width")) {
            pads.add(drafting.surfacePad(String.valueOf(leads.size() + 1), 0, 0, outline.length("exposed-pad-width"),
                    outline.length("exposed-pad-length")));
        }

        double halfWidth = bodyWidth / 2;
        double halfLength = bodyLength / 2;
        double padOuter = centre + padLength / 2;
        // The first lead marks the end its line reaches out over; a lead stands at the top end in every table.
        double lineY = Math.max(halfLength + drawing.silkscreenOffset(), Math.abs(firstY) + padWidth / 2 + drawing.clearOfCopper());
        List<FootprintOutline> outlines = List.of(
                drafting.body(bodyWidth, bodyLength),
                drafting.silkscreen("M" + point(-padOuter, -lineY) + "H" + number(halfWidth)
                        + "M" + point(-halfWidth, lineY) + "H" + number(halfWidth)),
                drafting.courtyard(Math.max(padOuter, halfWidth) + fillets.courtyard(),
                        Math.max(halfLength, lastPadEdge) + fillets.courtyard()));

        String slug = name + "-footprint";
        String title = designation + ", " + number(bodyWidth) + " mm body";
        String summary = leads.size() + "-lead " + outline.title() + ", gull-wing, " + number(pitch) + " mm pitch, body "
                + number(bodyWidth) + " × " + number(bodyLength) + " mm";
        List<String> notes = List.of(
                "Generated from footprint-standards/small-outline-packages.sht#" + outline.id() + " and the land pattern rules; regenerate, never edit.",
                "Leads span " + number(span.minimum()) + "…" + number(span.maximum()) + ", foot " + number(foot.minimum()) + "…"
                        + number(foot.maximum()) + ", width " + number(lead.minimum()) + "…" + number(lead.maximum())
                        + "; fillets toe " + number(fillets.toe()) + ", heel " + number(fillets.heel()) + ", side " + number(fillets.side()) + ".",
                "Z = " + number(land.outer()) + ", G = " + number(land.inner()) + ", X = " + number(land.width())
                        + "; pad " + number(padLength) + " × " + number(padWidth) + " centred at ±" + number(centre) + ".");
        return new StandardFootprint(slug, title, summary, outline.word("folder"), "surface", outline.standard(),
                rules.license(), notes, new Block.Footprint(pads, outlines));
    }

    /** The n-th lead of a row, the row centred on the origin. */
    static double rowY(int index, int count, double pitch) {
        return (index - (count - 1) / 2.0) * pitch;
    }
}
