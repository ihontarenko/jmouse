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

import static org.jmouse.cad.packages.Measures.written;

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
    public String form() {
        return "chip";
    }

    @Override
    public List<StandardFootprint> footprints(Sheet table, LandPatternRules rules) {
        List<StandardFootprint> footprints = new ArrayList<>();
        SheetEntry kinds = table.entry("kinds").orElseThrow(() -> new IllegalArgumentException("The chip table has no kinds"));
        for (SheetEntry kindEntry : kinds.entries()) {
            // A kind reads what every kind shares — the mark of hand soldering — from the group, as a size reads its kind.
            PackageTable kind = PackageTable.inheriting(kinds, kindEntry);
            PackageTable sizes = new PackageTable(table.entry(kind.word("sizes")).orElseThrow(() ->
                    new IllegalArgumentException("The chip kind '" + kind.id() + "' reads a size table that is not there")));
            for (SheetEntry sizeEntry : sizes.entry().entries()) {
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
        LandPatternRules.Drawing drawing = rules.drawing();
        FootprintDrafting drafting = new FootprintDrafting(drawing);
        SurfaceLand land = SurfaceLand.of(length, terminal, width, fillets, rules.tolerances());
        double padLength = land.padLength(drawing);
        double centre = land.centre(drawing);
        double padWidth = land.padWidth(drawing);
        List<Pad> pads = List.of(
                drafting.surfacePad("1", -centre, 0, padLength, padWidth),
                drafting.surfacePad("2", centre, 0, padLength, padWidth));
        List<Outline> outlines = new ArrayList<>();
        // A leaded body (SOD) writes its leads as length, terminal and width, and its body apart.
        double bodyLength = size.has("body-length") ? size.length("body-length") : length.nominal();
        double bodyWidth = size.has("body-width") ? size.length("body-width") : width.nominal();
        outlines.add(drafting.plainBody(bodyLength, bodyWidth));
        double lineEnd = centre - padLength / 2 - drawing.silkscreenClearance() - drawing.silkscreenWidth() / 2;
        double lineY = Math.max(bodyWidth / 2 + drawing.silkscreenOffset(), padWidth / 2 + drawing.clearOfCopper());
        if (lineEnd > drawing.silkscreenWidth()) {
            outlines.add(drafting.silkscreen(MeasuredPath.path().moveTo(-lineEnd, -lineY).horizontalTo(lineEnd)
                    .moveTo(-lineEnd, lineY).horizontalTo(lineEnd)));
        }
        boolean polarised = "cathode".equals(kind.optionalWord("polarity").orElse(""));
        double padOuter = centre + padLength / 2;
        if (polarised) {
            double barX = padOuter + drawing.silkscreenClearance() + drawing.silkscreenWidth() / 2;
            double barY = Math.max(padWidth / 2, width.nominal() / 2) + drawing.silkscreenOffset();
            outlines.add(drafting.silkscreen(MeasuredPath.path().moveTo(-barX, -barY).verticalTo(barY)));
        }
        outlines.add(drafting.courtyard(Math.max(padOuter, length.nominal() / 2) + fillets.courtyard(),
                Math.max(padWidth / 2, width.nominal() / 2) + fillets.courtyard()));
        String kindTitle = kind.title();
        String name = kind.optionalWord("name").orElse("chip-" + kind.id());
        String slug = name + (code.isEmpty() ? "" : "-" + code) + (handSoldering ? "-hand-soldering" : "") + "-footprint";
        // The code from the kind's template: the size's code, and whether its pads are lengthened for an iron.
        Map<String, String> words = new LinkedHashMap<>(size.words());
        words.put("code", code);
        words.put("hand", handSoldering ? kind.word("hand-soldering-mark") : "");
        String designated = PackageTable.designated(kind.word("designation"), words);
        String title = kindTitle + " " + size.title() + (handSoldering ? ", hand soldering" : "");
        String summary = kindTitle + ", " + size.title() + " body, " + (handSoldering
                ? "pads lengthened for a soldering iron" : "reflow pads") + (polarised ? "; pad 1 is the cathode" : "");
        List<String> notes = List.of(
                "Generated from footprint-standards/chip-packages.sht#" + size.id() + " and the land pattern rules; regenerate, never edit.",
                "Body " + written(length.minimum()) + "…" + written(length.maximum()) + " × " + written(width.minimum()) + "…"
                        + written(width.maximum()) + ", terminal " + written(terminal.minimum()) + "…" + written(terminal.maximum())
                        + "; fillets toe " + written(fillets.toe()) + ", heel " + written(fillets.heel()) + ", side " + written(fillets.side()) + ".",
                "Z = " + written(land.outer()) + ", G = " + written(land.inner()) + ", X = " + written(land.width())
                        + "; pad " + written(padLength) + " × " + written(padWidth) + " centred at ±" + written(centre) + ".");
        return new StandardFootprint(slug, designated, title, summary, kind.word("folder"), "surface",
                sizes.standard() + ", " + size.title(), rules, notes, new FootprintDrawing(pads, outlines));
    }
}
