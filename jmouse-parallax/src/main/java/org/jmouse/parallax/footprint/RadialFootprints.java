package org.jmouse.parallax.footprint;

import org.jmouse.datamap.DataMap;
import org.jmouse.datamap.DataMapEntry;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.FootprintOutline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.jmouse.parallax.footprint.FootprintDrafting.circle;
import static org.jmouse.parallax.footprint.FootprintDrafting.number;
import static org.jmouse.parallax.footprint.FootprintDrafting.point;

/**
 * Bodies standing over two leads in a row, seen from above: a {@code circle} (an aluminium can, a round LED) or a
 * {@code rectangle} (a ceramic disc, a film box). The silkscreen outlines the body, widened where it would touch a pad;
 * {@code polarity positive} marks pin 1 with a plus, {@code cathode} with a flat — pin 1 is square either way.
 */
final class RadialFootprints implements FootprintFamily {

    /** How long each arm of a polarity plus is, against the silkscreen line's width. */
    private static final double PLUS_ARMS = 4;

    @Override
    public List<StandardFootprint> footprints(DataMap table, LandPatternRules rules) {
        List<StandardFootprint> footprints = new ArrayList<>();
        for (DataMapEntry kind : table.entries()) {
            String kindTitle = kind.title().orElse(kind.id());
            for (DataMapEntry sizeEntry : kind.entries()) {
                PackageTable size = PackageTable.inheriting(kind, sizeEntry);
                if (size.has("widths")) {
                    for (double width : size.lengths("widths")) {
                        footprints.add(footprint(kindTitle, size, width, rules));
                    }
                } else {
                    footprints.add(footprint(kindTitle, size, size.has("width") ? size.length("width") : 0, rules));
                }
            }
        }
        return footprints;
    }

    private StandardFootprint footprint(String kindTitle, PackageTable size, double width, LandPatternRules rules) {
        LandPatternRules.ThroughHole holes = rules.throughHole();
        LandPatternRules.Drawing drawing = rules.drawing();
        FootprintDrafting drafting = new FootprintDrafting(drawing);
        double pitch = size.length("pitch");
        double drill = holes.drill(size.length("lead"), 0);
        double pad = holes.pad(drill, pitch);
        double end = pitch / 2;
        double padReach = end + pad / 2 + drawing.clearOfCopper();
        boolean round = "circle".equals(size.word("outline"));
        String polarity = size.optionalWord("polarity").orElse("none");

        List<Block.FootprintPad> pads = List.of(drafting.throughHolePad("1", -end, 0, pad, drill),
                drafting.throughHolePad("2", end, 0, pad, drill));
        List<FootprintOutline> outlines = new ArrayList<>();
        double halfX;
        double halfY;
        if (round) {
            double radius = size.length("diameter") / 2;
            double silk = Math.max(radius + drawing.silkscreenOffset(), padReach);
            outlines.add(drafting.fabrication(circle(0, 0, radius)));
            outlines.add(drafting.silkscreen(circle(0, 0, silk)));
            if (size.has("body")) {
                outlines.add(drafting.fabrication(circle(0, 0, size.length("body") / 2)));
            }
            halfX = Math.max(radius, end + pad / 2);
            halfY = Math.max(radius, pad / 2);
            if ("cathode".equals(polarity)) {
                double flat = -Math.sqrt(Math.max(0, radius * radius - Math.pow(radius * 0.8, 2)));
                outlines.add(drafting.fabrication("M" + point(-radius * 0.6, flat) + "V" + number(-flat)));
            }
        } else {
            double length = size.length("length");
            outlines.add(drafting.fabrication(drafting.rectangle(length / 2, width / 2)));
            outlines.add(drafting.silkscreen(drafting.rectangle(Math.max(length / 2 + drawing.silkscreenOffset(), padReach),
                    Math.max(width / 2 + drawing.silkscreenOffset(), pad / 2 + drawing.clearOfCopper()))));
            halfX = Math.max(length / 2, end + pad / 2);
            halfY = Math.max(width / 2, pad / 2);
        }
        if ("positive".equals(polarity)) {
            double arm = PLUS_ARMS * drawing.silkscreenWidth();
            double x = -end - pad / 2 - drawing.clearOfCopper() - arm;
            double y = -pad / 2 - drawing.clearOfCopper() - arm;
            outlines.add(drafting.silkscreen("M" + point(x - arm, y) + "H" + number(x + arm)
                    + "M" + point(x, y - arm) + "V" + number(y + arm)));
        }
        outlines.add(drafting.courtyard(halfX + holes.courtyard(), halfY + holes.courtyard()));

        Map<String, String> words = new LinkedHashMap<>(size.words());
        if (width > 0) {
            words.put("width", PackageTable.hundredths(width));
        }
        String slug = PackageTable.named(size.word("name"), words) + "-footprint";
        String title = kindTitle + ", " + size.title() + (width > 0 && size.has("widths") ? ", " + number(width) + " mm wide" : "");
        String summary = title + ", leads " + number(pitch) + " mm apart"
                + ("positive".equals(polarity) ? "; pin 1 is the positive lead" : "cathode".equals(polarity) ? "; pin 1 is the cathode" : "");
        List<String> notes = List.of(
                "Generated from footprint-standards/radial-packages.jmd#" + size.id() + " and the land pattern rules; regenerate, never edit.",
                "Lead ⌀" + number(size.length("lead")) + ": drill " + number(drill) + ", pad " + number(pad) + ", pin 1 square.");
        return new StandardFootprint(slug, title, summary, size.word("folder"), "through-hole", size.standard(),
                rules.license(), notes, new Block.Footprint(pads, outlines));
    }
}
