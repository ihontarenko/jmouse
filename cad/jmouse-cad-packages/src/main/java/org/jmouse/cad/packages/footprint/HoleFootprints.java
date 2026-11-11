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
 * What a board needs that no part brings: mounting holes for metric screws — bare, ringed under the screw's head,
 * ringed and stitched by vias, ringed under a countersunk head — and round test pads a probe lands on.
 */
final class HoleFootprints implements FootprintFamily {

    @Override
    public String form() {
        return "hole";
    }

    @Override
    public List<StandardFootprint> footprints(Sheet table, LandPatternRules rules) {
        List<StandardFootprint> footprints = new ArrayList<>();
        for (SheetEntry kind : table.entries()) {
            if (kind.value("diameters").isPresent()) {
                PackageTable testPoints = new PackageTable(kind);
                for (double diameter : testPoints.lengths("diameters")) {
                    footprints.add(testPoint(testPoints, diameter, rules));
                }
                continue;
            }
            for (SheetEntry screwEntry : kind.entries()) {
                PackageTable screw = PackageTable.inheriting(kind, screwEntry);
                footprints.add(mountingHole(screw, "", null, false, rules));
                footprints.add(mountingHole(screw, "-pad", "head", false, rules));
                footprints.add(mountingHole(screw, "-pad-via", "head", true, rules));
                footprints.add(mountingHole(screw, "-din-965-pad", "countersunk-head", false, rules));
            }
        }
        return footprints;
    }

    /** A hole for the screw; with {@code ring} naming a head, plated with a pad that head covers. */
    private StandardFootprint mountingHole(PackageTable screw, String variant, String ring, boolean vias,
                                           LandPatternRules rules) {
        LandPatternRules.Drawing drawing = rules.drawing();
        FootprintDrafting drafting = new FootprintDrafting(drawing);
        double hole = screw.length("hole");
        double head = screw.length(ring == null ? "head" : ring);
        double pad = head + 2 * screw.length("head-clearance");
        List<Pad> pads = new ArrayList<>();
        if (ring == null) {
            pads.add(drafting.unplatedHole(hole));
        } else {
            pads.add(drafting.platedHole("1", 0, 0, pad, hole));
        }
        if (vias) {
            int count = (int) screw.number("vias");
            double radius = (hole / 2 + pad / 2) / 2;
            for (int index = 0; index < count; index++) {
                double angle = 2 * Math.PI * index / count;
                pads.add(drafting.platedHole("1", radius * Math.cos(angle), radius * Math.sin(angle),
                        screw.length("via-pad"), screw.length("via-drill")));
            }
        }
        double outer = ring == null ? head : pad;
        List<Outline> outlines = List.of(
                drafting.fabrication(MeasuredPath.path().circle(0, 0, head / 2)),
                drafting.silkscreen(MeasuredPath.path().circle(0, 0, outer / 2 + drawing.clearOfCopper())),
                drafting.courtyardCircle(Math.max(head, outer) / 2 + rules.throughHole().courtyard()));
        String slug = PackageTable.named(screw.word("name"), screw.words()) + variant + "-footprint";
        Map<String, String> words = new LinkedHashMap<>(screw.words());
        words.put("variant", variant);
        String designated = PackageTable.designated(screw.word("designation"), words);
        String how = ring == null ? "a bare hole" : "countersunk-head".equals(ring) ? "a plated ring under a countersunk head"
                : vias ? "a plated ring under the head, stitched by vias" : "a plated ring under the head";
        String title = "Mounting hole " + screw.title() + (ring == null ? "" : ", " + how);
        String summary = screw.title() + " screw, " + written(hole) + " mm hole, " + how;
        List<String> notes = List.of(
                "Generated from footprint-standards/hole-packages.sht#" + screw.id() + "; regenerate, never edit.",
                "Hole " + written(hole) + " (ISO 273 fine); head ⌀" + written(head)
                        + (ring == null ? "" : ", ring ⌀" + written(pad) + " = head + 2 × " + written(screw.length("head-clearance"))) + ".");
        return new StandardFootprint(slug, designated, title, summary, screw.word("folder"), "through-hole", screw.standard(),
                rules, notes, new FootprintDrawing(pads, outlines));
    }

    private StandardFootprint testPoint(PackageTable testPoints, double diameter, LandPatternRules rules) {
        LandPatternRules.Drawing drawing = rules.drawing();
        FootprintDrafting drafting = new FootprintDrafting(drawing);
        LandPatternRules.Fillets fillets = rules.fillets("test-point");
        double radius = diameter / 2;
        List<Outline> outlines = List.of(
                drafting.silkscreen(MeasuredPath.path().circle(0, 0, radius + drawing.clearOfCopper())),
                drafting.courtyardCircle(radius + drawing.clearOfCopper() + fillets.courtyard()));
        Map<String, String> words = new LinkedHashMap<>(testPoints.words());
        words.put("diameter", PackageTable.hundredths(diameter));
        String slug = PackageTable.named(testPoints.word("name"), words) + "-footprint";
        String designated = PackageTable.designated(testPoints.word("designation"), words);
        String title = "Test point, " + written(diameter) + " mm pad";
        String summary = "A bare round pad " + written(diameter) + " mm across, for a probe";
        List<String> notes = List.of("Generated from footprint-standards/hole-packages.sht#" + testPoints.id() + "; regenerate, never edit.");
        return new StandardFootprint(slug, designated, title, summary, testPoints.word("folder"), "surface", testPoints.standard(),
                rules, notes, new FootprintDrawing(List.of(drafting.roundSurfacePad("1", diameter)), outlines));
    }
}
