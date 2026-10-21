package org.jmouse.parallax.footprint;

import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.FootprintOutline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.jmouse.parallax.footprint.FootprintDrafting.circle;
import static org.jmouse.parallax.footprint.FootprintDrafting.number;
import static org.jmouse.parallax.footprint.FootprintDrafting.point;

/**
 * Cylindrical bodies with a lead out of each end: laid flat between two holes at every pitch that leaves the lead its
 * straight stretch before the bend, or stood on end over pin 1. A kind with {@code polarity cathode} has pin 1 as its
 * cathode and a band drawn at that end.
 */
final class AxialFootprints implements FootprintFamily {

    @Override
    public List<StandardFootprint> footprints(Sheet table, LandPatternRules rules) {
        List<StandardFootprint> footprints = new ArrayList<>();
        for (SheetEntry kind : table.entries()) {
            String kindTitle = kind.title().orElse(kind.id());
            for (SheetEntry sizeEntry : kind.entries()) {
                PackageTable size = PackageTable.inheriting(kind, sizeEntry);
                double reach = size.length("length") + 2 * size.length("bend");
                for (double pitch : size.lengths("pitches")) {
                    if (pitch >= reach) {
                        footprints.add(lying(kindTitle, size, pitch, rules));
                    }
                }
                for (double pitch : size.lengths("standing-pitches")) {
                    standing(kindTitle, size, pitch, rules).ifPresent(footprints::add);
                }
            }
        }
        return footprints;
    }

    private StandardFootprint lying(String kindTitle, PackageTable size, double pitch, LandPatternRules rules) {
        LandPatternRules.ThroughHole holes = rules.throughHole();
        LandPatternRules.Drawing drawing = rules.drawing();
        FootprintDrafting drafting = new FootprintDrafting(drawing);
        double length = size.length("length");
        double diameter = size.length("diameter");
        double drill = holes.drill(size.length("lead"), 0);
        double pad = holes.pad(drill, pitch);
        double end = pitch / 2;

        List<Block.FootprintPad> pads = List.of(drafting.throughHolePad("1", -end, 0, pad, drill),
                drafting.throughHolePad("2", end, 0, pad, drill));
        double halfBody = length / 2;
        double silkX = halfBody + drawing.silkscreenOffset();
        double silkY = diameter / 2 + drawing.silkscreenOffset();
        double leadStop = end - pad / 2 - drawing.clearOfCopper();
        List<FootprintOutline> outlines = new ArrayList<>();
        outlines.add(drafting.fabrication(drafting.rectangle(halfBody, diameter / 2)
                + "M" + point(-end, 0) + "H" + number(-halfBody) + "M" + point(halfBody, 0) + "H" + number(end)));
        StringBuilder silk = new StringBuilder(drafting.rectangle(silkX, silkY));
        if (leadStop > silkX + drawing.silkscreenWidth()) {
            silk.append("M").append(point(-leadStop, 0)).append("H").append(number(-silkX))
                    .append("M").append(point(silkX, 0)).append("H").append(number(leadStop));
        }
        if (cathode(size)) {
            double band = -halfBody + length / 6;
            silk.append("M").append(point(band, -silkY)).append("V").append(number(silkY));
            outlines.add(drafting.fabrication("M" + point(band, -diameter / 2) + "V" + number(diameter / 2)));
        }
        outlines.add(drafting.silkscreen(silk.toString()));
        outlines.add(drafting.courtyard(Math.max(end + pad / 2, halfBody) + holes.courtyard(),
                Math.max(pad, diameter) / 2 + holes.courtyard()));

        return footprint(kindTitle, size, pitch, "horizontal", pads, outlines, drill, pad,
                "laid flat, " + number(pitch) + " mm between its holes", rules);
    }

    /** Stood on end over pin 1; the body must leave pin 2's pad clear, or there is no such footprint. */
    private Optional<StandardFootprint> standing(String kindTitle, PackageTable size, double pitch, LandPatternRules rules) {
        LandPatternRules.ThroughHole holes = rules.throughHole();
        LandPatternRules.Drawing drawing = rules.drawing();
        FootprintDrafting drafting = new FootprintDrafting(drawing);
        double diameter = size.length("diameter");
        double drill = holes.drill(size.length("lead"), 0);
        double pad = holes.pad(drill, pitch);
        double radius = Math.max(diameter / 2, pad / 2) + drawing.silkscreenOffset();
        if (radius + drawing.clearOfCopper() > pitch - pad / 2) {
            return Optional.empty();
        }
        double end = pitch / 2;
        List<Block.FootprintPad> pads = List.of(drafting.throughHolePad("1", -end, 0, pad, drill),
                drafting.throughHolePad("2", end, 0, pad, drill));
        List<FootprintOutline> outlines = new ArrayList<>();
        outlines.add(drafting.fabrication(circle(-end, 0, diameter / 2) + "M" + point(-end, 0) + "H" + number(end)));
        outlines.add(drafting.silkscreen(circle(-end, 0, radius)));
        outlines.add(drafting.courtyard(-end - radius - holes.courtyard(), -radius - holes.courtyard(),
                end + pad / 2 + holes.courtyard(), radius + holes.courtyard()));
        return Optional.of(footprint(kindTitle, size, pitch, "vertical", pads, outlines, drill, pad,
                "stood on end over pin 1, " + number(pitch) + " mm between its holes", rules));
    }

    private StandardFootprint footprint(String kindTitle, PackageTable size, double pitch, String standing, List<Block.FootprintPad> pads,
                                        List<FootprintOutline> outlines, double drill, double pad, String how,
                                        LandPatternRules rules) {
        Map<String, String> words = new LinkedHashMap<>(size.words());
        words.put("pitch", PackageTable.hundredths(pitch));
        words.put("standing", standing);
        String slug = PackageTable.named(size.word("name"), words) + "-footprint";
        String title = kindTitle + " " + size.title();
        String summary = kindTitle + " " + size.title() + ", body " + number(size.length("length")) + " × ⌀" + number(size.length("diameter"))
                + " mm, " + how + (cathode(size) ? "; pin 1 is the cathode" : "");
        List<String> notes = List.of(
                "Generated from footprint-standards/axial-packages.sht#" + size.id() + " and the land pattern rules; regenerate, never edit.",
                "Lead ⌀" + number(size.length("lead")) + ": drill " + number(drill) + ", pad " + number(pad) + ", pin 1 square.");
        return new StandardFootprint(slug, title + ", " + standing + ", " + number(pitch) + " mm", summary,
                size.word("folder"), "through-hole", size.standard(), rules.license(), notes, new Block.Footprint(pads, outlines));
    }

    private static boolean cathode(PackageTable size) {
        return "cathode".equals(size.optionalWord("polarity").orElse(""));
    }
}
