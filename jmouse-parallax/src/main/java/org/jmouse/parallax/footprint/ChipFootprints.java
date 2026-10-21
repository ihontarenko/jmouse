package org.jmouse.parallax.footprint;

import org.jmouse.datamap.DataMap;
import org.jmouse.datamap.DataMapEntry;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.FootprintOutline;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.parallax.footprint.FootprintDrafting.number;

/**
 * Two-terminal parts with end terminals — chip resistors, capacitors, inductors, fuses, diodes, LEDs by EIA size, and
 * MELF cylinders —: for every kind and every size of the body table it reads, a reflow footprint and, unless the kind
 * says {@code hand-soldering no}, a hand-soldering one. A kind names its fillets ({@code chip} unless written); a body
 * shorter than 1608 takes the {@code -small} set where the rules have one. A kind with {@code polarity cathode} has
 * pad 1 as its cathode, marked on the silkscreen.
 */
final class ChipFootprints implements FootprintFamily {

    /** Below this body length (1608 metric) the smaller parts' fillets apply. */
    private static final double SMALL_BODY = 1.55;

    @Override
    public List<StandardFootprint> footprints(DataMap table, LandPatternRules rules) {
        List<StandardFootprint> footprints = new ArrayList<>();
        DataMapEntry kinds = table.entry("kinds").orElseThrow(() -> new IllegalArgumentException("The chip table has no kinds"));
        for (DataMapEntry kindEntry : kinds.entries()) {
            PackageTable kind = new PackageTable(kindEntry);
            PackageTable sizes = new PackageTable(table.entry(kind.word("sizes")).orElseThrow(() ->
                    new IllegalArgumentException("The chip kind '" + kind.id() + "' reads a size table that is not there")));
            for (DataMapEntry sizeEntry : sizes.entry().entries()) {
                PackageTable size = new PackageTable(sizeEntry);
                footprints.add(footprint(kind, sizes, size, false, rules));
                if (!"no".equals(kind.optionalWord("hand-soldering").orElse("yes"))) {
                    footprints.add(footprint(kind, sizes, size, true, rules));
                }
            }
        }
        return footprints;
    }

    private StandardFootprint footprint(PackageTable kind, PackageTable sizes, PackageTable size, boolean handSoldering,
                                        LandPatternRules rules) {
        String code = size.optionalWord("code").orElse(size.id().substring(size.id().lastIndexOf('-') + 1));
        PackageTable.Span length = size.span("length");
        PackageTable.Span width = size.span("width");
        PackageTable.Span terminal = size.span("terminal");
        boolean small = length.nominal() < SMALL_BODY;
        String filletsName = kind.optionalWord("fillets").orElse("chip");
        String sized = small && rules.find(filletsName + "-small").isPresent() ? filletsName + "-small" : filletsName;
        LandPatternRules.Fillets fillets = rules.fillets(handSoldering ? sized + "-hand-soldering" : sized);
        FootprintDrafting drafting = new FootprintDrafting(rules.drawing());
        LandPatternRules.Drawing drawing = rules.drawing();

        SurfaceLand land = SurfaceLand.of(length, terminal, width, fillets, rules.tolerances());
        double padLength = land.padLength(drawing);
        double centre = land.centre(drawing);
        double padWidth = land.padWidth(drawing);

        List<Block.FootprintPad> pads = List.of(
                drafting.surfacePad("1", -centre, 0, padLength, padWidth),
                drafting.surfacePad("2", centre, 0, padLength, padWidth));
        List<FootprintOutline> outlines = new ArrayList<>();
        outlines.add(drafting.plainBody(length.nominal(), width.nominal()));
        double lineEnd = centre - padLength / 2 - drawing.silkscreenClearance() - drawing.silkscreenWidth() / 2;
        double lineY = width.nominal() / 2 + drawing.silkscreenOffset();
        if (lineEnd > drawing.silkscreenWidth()) {
            outlines.add(drafting.silkscreen("M" + FootprintDrafting.point(-lineEnd, -lineY) + "H" + number(lineEnd)
                    + "M" + FootprintDrafting.point(-lineEnd, lineY) + "H" + number(lineEnd)));
        }
        boolean polarised = "cathode".equals(kind.optionalWord("polarity").orElse(""));
        double padOuter = centre + padLength / 2;
        if (polarised) {
            double barX = padOuter + drawing.silkscreenClearance() + drawing.silkscreenWidth() / 2;
            double barY = Math.max(padWidth / 2, width.nominal() / 2) + drawing.silkscreenOffset();
            outlines.add(drafting.silkscreen("M" + FootprintDrafting.point(-barX, -barY) + "V" + number(barY)));
        }
        outlines.add(drafting.courtyard(Math.max(padOuter, length.nominal() / 2) + fillets.courtyard(),
                Math.max(padWidth / 2, width.nominal() / 2) + fillets.courtyard()));

        String kindTitle = kind.title();
        String name = kind.optionalWord("name").orElse("chip-" + kind.id());
        String slug = name + (code.isEmpty() ? "" : "-" + code) + (handSoldering ? "-hand-soldering" : "") + "-footprint";
        String title = kindTitle + " " + size.title() + (handSoldering ? ", hand soldering" : "");
        String summary = kindTitle + ", " + size.title() + " body, " + (handSoldering
                ? "pads lengthened for a soldering iron" : "reflow pads") + (polarised ? "; pad 1 is the cathode" : "");
        List<String> notes = List.of(
                "Generated from footprint-standards/chip-packages.jmd#" + size.id() + " and the land pattern rules; regenerate, never edit.",
                "Body " + number(length.minimum()) + "…" + number(length.maximum()) + " × " + number(width.minimum()) + "…"
                        + number(width.maximum()) + ", terminal " + number(terminal.minimum()) + "…" + number(terminal.maximum())
                        + "; fillets toe " + number(fillets.toe()) + ", heel " + number(fillets.heel()) + ", side " + number(fillets.side()) + ".",
                "Z = " + number(land.outer()) + ", G = " + number(land.inner()) + ", X = " + number(land.width())
                        + "; pad " + number(padLength) + " × " + number(padWidth) + " centred at ±" + number(centre) + ".");
        return new StandardFootprint(slug, title, summary, kind.word("folder"), "surface",
                sizes.standard() + ", " + size.title(), rules.license(), notes, new Block.Footprint(pads, outlines));
    }
}
