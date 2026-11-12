package org.jmouse.cad.packages.footprint;

import org.jmouse.cad.packages.MeasuredPath;
import org.jmouse.cad.packages.PackageTable;
import org.jmouse.cad.packages.footprint.FootprintDrawing.Outline;
import org.jmouse.cad.packages.footprint.FootprintDrawing.Pad;
import org.jmouse.sheet.Sheet;
import org.jmouse.sheet.SheetEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jmouse.cad.packages.Measures.written;

/**
 * Bodies whose leads stand where their maker put them, on no row a rule describes — a relay's coil and contacts, a
 * printed-circuit transformer's two rows of windings, a round bridge rectifier's square of leads —: each lead at the
 * place its table writes ({@code pin-x}, {@code pin-y}, in the maker's frame), the body a box or a round
 * ({@code outline round}) at {@code body-x}, {@code body-y}. A surface part ({@code mount surface}) — a four-terminal shunt —
 * has lands of its maker's own sizes ({@code pad-width}, {@code pad-height}). Drawn about the body's centre, so the part
 * turns about it.
 */
final class PinnedFootprints implements FootprintFamily {

    private static final String ROUND = "round";
    private static final String SURFACE = "surface";
    /** The fillet rules a surface part's courtyard is spaced by: its lands are its maker's, its keep-off the rules'. */
    private static final String SURFACE_FILLETS = "chip";

    @Override
    public String form() {
        return "pinned";
    }

    @Override
    public List<StandardFootprint> footprints(Sheet table, LandPatternRules rules) {
        List<StandardFootprint> footprints = new ArrayList<>();
        for (SheetEntry entry : table.entries()) {
            footprints.add(footprint(new PackageTable(entry), rules));
        }
        return footprints;
    }

    private StandardFootprint footprint(PackageTable outline, LandPatternRules rules) {
        LandPatternRules.ThroughHole holes = rules.throughHole();
        LandPatternRules.Drawing drawing = rules.drawing();
        FootprintDrafting drafting = new FootprintDrafting(drawing);
        Map<String, Double> xs = outline.lengthsByName("pin-x");
        Map<String, Double> ys = outline.lengthsByName("pin-y");
        if (!xs.keySet().equals(ys.keySet())) {
            throw new IllegalArgumentException(outline.id() + ": pin-x and pin-y name different pins");
        }
        double centreX = outline.length("body-x");
        double centreY = outline.length("body-y");
        boolean surface = outline.optionalWord("mount").filter(SURFACE::equals).isPresent();
        double drill = surface ? 0 : holes.drill(outline.length("lead"), outline.length("thickness"));
        double pad = surface ? 0 : holes.pad(drill);
        Map<String, Double> widths = surface ? outline.lengthsByName("pad-width") : Map.of();
        Map<String, Double> heights = surface ? outline.lengthsByName("pad-height") : Map.of();
        List<Pad> pads = new ArrayList<>();
        // A surface part's lands are its maker's, each its own size; a lead's hole and ring are the rules'.
        xs.forEach((number, x) -> pads.add(surface
                ? drafting.surfacePad(number, x - centreX, ys.get(number) - centreY, widths.get(number), heights.get(number))
                : drafting.throughHolePad(number, x - centreX, ys.get(number) - centreY, pad, drill)));
        double halfWidth = outline.length("body-width") / 2;
        double halfDepth = outline.length("body-depth") / 2;
        boolean round = outline.optionalWord("outline").filter(ROUND::equals).isPresent();
        double offset = drawing.silkscreenOffset();
        List<Outline> outlines = new ArrayList<>();
        // How far out the silkscreen stands across the body: the courtyard keeps it inside too.
        double silkscreenEdge = halfDepth + offset;
        outlines.add(drafting.fabrication(round ? MeasuredPath.path().circle(0, 0, halfWidth) : MeasuredPath.path().rectangle(halfWidth, halfDepth)));
        if (surface) {
            // Lands reach past the body: the silkscreen is the body's two long edges, clear of every land.
            double clear = pads.stream().mapToDouble(each -> Math.abs(each.y()) + each.height() / 2).max().orElse(0)
                    + drawing.clearOfCopper() + drawing.silkscreenWidth() / 2;
            silkscreenEdge = Math.max(halfDepth + offset, clear);
            outlines.add(drafting.silkscreen(MeasuredPath.path().moveTo(-halfWidth, -silkscreenEdge).horizontalTo(halfWidth)
                    .moveTo(-halfWidth, silkscreenEdge).horizontalTo(halfWidth)));
        } else {
            outlines.add(drafting.silkscreen(round ? MeasuredPath.path().circle(0, 0, halfWidth + offset)
                    : MeasuredPath.path().rectangle(halfWidth + offset, halfDepth + offset)));
        }
        // Pin 1 is marked beside its pad, on the side away from the body's middle.
        Pad first = pads.getFirst();
        double away = Math.signum(first.x()) == 0 ? -1 : Math.signum(first.x());
        outlines.add(drafting.pinMark(first.x() + away * (first.width() / 2 + drawing.clearOfCopper() + drawing.pinMarkRadius()), first.y()));
        double padsLeft = pads.stream().mapToDouble(each -> each.x() - each.width() / 2).min().orElse(0);
        double padsRight = pads.stream().mapToDouble(each -> each.x() + each.width() / 2).max().orElse(0);
        double padsTop = pads.stream().mapToDouble(each -> each.y() - each.height() / 2).min().orElse(0);
        double padsBottom = pads.stream().mapToDouble(each -> each.y() + each.height() / 2).max().orElse(0);
        double courtyard = surface ? rules.fillets(SURFACE_FILLETS).courtyard() : holes.courtyard();
        double across = Math.max(silkscreenEdge, halfDepth);
        outlines.add(drafting.courtyard(Math.min(-halfWidth, padsLeft) - courtyard, Math.min(-across, padsTop) - courtyard,
                Math.max(halfWidth, padsRight) + courtyard, Math.max(across, padsBottom) + courtyard));
        String slug = outline.word("name") + "-footprint";
        String designated = PackageTable.designated(outline.word("designation"), outline.words());
        String summary = pads.size() + (surface ? "-terminal " : "-lead ") + outline.word("part") + (surface ? ", surface-mount, " : ", through-hole, ")
                + (round ? written(2 * halfWidth) + " mm round" : written(2 * halfWidth) + " × " + written(2 * halfDepth) + " mm") + " body";
        List<String> notes = List.of(
                "Generated from footprint-standards/pinned-packages.sht#" + outline.id() + " and the land pattern rules; regenerate, never edit.",
                surface ? "Each land where its maker puts it and of the size its maker recommends."
                        : "Lead " + written(outline.length("lead")) + " × " + written(outline.length("thickness")) + " at most: drill "
                        + written(drill) + ", pad " + written(pad) + ", pin 1 square; each lead where its maker puts it.");
        return new StandardFootprint(slug, designated, outline.title(), summary, outline.word("folder"), outline.word("category"), surface ? "surface" : "through-hole",
                outline.standard(), rules, notes, new FootprintDrawing(pads, outlines, outline));
    }

}
