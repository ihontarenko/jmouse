package org.jmouse.parallax.footprint;

import org.jmouse.datamap.DataMap;
import org.jmouse.datamap.DataMapEntry;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.FootprintOutline;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.parallax.footprint.FootprintDrafting.number;

/**
 * Two-terminal chip parts — resistors, capacitors, inductors, fuses, diodes, LEDs — by EIA size: for every kind and
 * every size of the body table it reads, a reflow footprint and a hand-soldering one. A diode's and an LED's pad 1 is
 * the cathode, marked on the silkscreen.
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
                footprints.add(footprint(kind, sizes, size, true, rules));
            }
        }
        return footprints;
    }

    private StandardFootprint footprint(PackageTable kind, PackageTable sizes, PackageTable size, boolean handSoldering,
                                        LandPatternRules rules) {
        String code = size.id().substring(size.id().lastIndexOf('-') + 1);
        PackageTable.Span length = size.span("length");
        PackageTable.Span width = size.span("width");
        PackageTable.Span terminal = size.span("terminal");
        boolean small = length.nominal() < SMALL_BODY;
        LandPatternRules.Fillets fillets = small
                ? handSoldering ? rules.chipSmallHandSoldering() : rules.chipSmall()
                : handSoldering ? rules.chipHandSoldering() : rules.chip();
        FootprintDrafting drafting = new FootprintDrafting(rules.drawing());
        LandPatternRules.Drawing drawing = rules.drawing();

        double outer = length.maximum() + 2 * fillets.toe();
        double inner = length.minimum() - 2 * terminal.maximum() - 2 * fillets.heel();
        double padLength = drawing.nearest((outer - inner) / 2);
        double centre = drawing.nearest((outer + inner) / 4);
        double padWidth = drawing.up(width.maximum() + 2 * fillets.side());

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
        boolean polarised = "diode".equals(kind.id()) || "led".equals(kind.id());
        double padOuter = centre + padLength / 2;
        if (polarised) {
            double barX = padOuter + drawing.silkscreenClearance() + drawing.silkscreenWidth() / 2;
            double barY = Math.max(padWidth / 2, width.nominal() / 2) + drawing.silkscreenOffset();
            outlines.add(drafting.silkscreen("M" + FootprintDrafting.point(-barX, -barY) + "V" + number(barY)));
        }
        outlines.add(drafting.courtyard(Math.max(padOuter, length.nominal() / 2) + fillets.courtyard(),
                Math.max(padWidth / 2, width.nominal() / 2) + fillets.courtyard()));

        String kindTitle = kind.title();
        String slug = "chip-" + kind.id() + "-" + code + (handSoldering ? "-hand-soldering" : "") + "-footprint";
        String title = "Chip " + kindTitle.toLowerCase() + " " + size.title() + (handSoldering ? ", hand soldering" : "");
        String summary = kindTitle + ", " + size.title() + " chip body, " + (handSoldering
                ? "pads lengthened for a soldering iron" : "reflow pads") + (polarised ? "; pad 1 is the cathode" : "");
        List<String> notes = List.of(
                "Generated from footprint-standards/chip-packages.jmd#" + size.id() + " and the land pattern rules; regenerate, never edit.",
                "Body " + number(length.minimum()) + "…" + number(length.maximum()) + " × " + number(width.minimum()) + "…"
                        + number(width.maximum()) + ", terminal " + number(terminal.minimum()) + "…" + number(terminal.maximum())
                        + "; fillets toe " + number(fillets.toe()) + ", heel " + number(fillets.heel()) + ", side " + number(fillets.side()) + ".",
                "Z = " + number(outer) + ", G = " + number(inner) + ", X = " + number(width.maximum() + 2 * fillets.side())
                        + "; pad " + number(padLength) + " × " + number(padWidth) + " centred at ±" + number(centre) + ".");
        return new StandardFootprint(slug, title, summary, kind.word("folder"), "surface",
                sizes.standard() + ", " + size.title(), rules.license(), notes, new Block.Footprint(pads, outlines));
    }
}
