package org.jmouse.cad.pcb.checks;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.geometry.index.Box;
import org.jmouse.geometry.index.BoxIndex;
import org.jmouse.geometry.index.BoxTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.jmouse.cad.document.FindingSeverity.WARNING;
import static org.jmouse.cad.pcb.geometry.CopperPieces.key;

/** Parts on one side whose courtyards overlap: the room a part needs to be placed and soldered is taken. */
final class CourtyardCheck implements PcbCheck {

    /** The box of a part with no courtyard: far from anything, so no courtyard is ever found near it. */
    private static final Box NO_COURTYARD = new Box(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);

    @Override
    public List<PcbFinding> findings(CheckContext context) {
        List<Placement> placements = context.pcb().placements();
        List<Optional<Box>> boxes = placements.stream().map(placement -> courtyardBox(context, placement)).toList();
        List<PcbFinding> findings = new ArrayList<>();
        // Parts with no courtyard stand nowhere: an empty box, which no other box comes near.
        BoxIndex courtyardsNear = BoxTree.of(boxes.stream().map(box -> box.orElse(NO_COURTYARD)).toList());
        for (int first = 0; first < boxes.size(); first++) {
            if (boxes.get(first).isEmpty()) {
                continue;
            }
            for (int second : courtyardsNear.near(boxes.get(first).get(), 0)) {
                if (second <= first) {
                    continue;
                }
                Placement placement = placements.get(first);
                Placement other = placements.get(second);
                if (boxes.get(second).isEmpty() || !placement.side().equals(other.side())) {
                    continue;
                }
                Box one = boxes.get(first).get();
                Box two = boxes.get(second).get();
                if (one.overlaps(two)) {
                    Point at = new Point((Math.max(one.left(), two.left()) + Math.min(one.right(), two.right())) / 2,
                            (Math.max(one.top(), two.top()) + Math.min(one.bottom(), two.bottom())) / 2);
                    findings.add(new PcbFinding(WARNING, placement.printed() + " and " + other.printed() + " crowd each other: their courtyards overlap",
                            List.of(key("placement", first), key("placement", second)), at));
                }
            }
        }
        return findings;
    }

    /** The box a placed part's courtyard covers; empty for a footprint that draws none. */
    private static Optional<Box> courtyardBox(CheckContext context, Placement placement) {
        List<FootprintDrawing.Outline> outlines = context.footprintOf(placement).map(footprint -> footprint.outlines().stream()
                .filter(outline -> PcbVocabulary.COURTYARD.equals(outline.layer())).toList()).orElse(List.of());
        Optional<Box> placed = Optional.empty();
        for (FootprintDrawing.Outline outline : outlines) {
            Optional<Box> drawn = context.pathBox(outline.path());
            if (drawn.isEmpty()) {
                continue;
            }
            for (Point corner : CheckShapes.cornersOf(drawn.get())) {
                Point turned = placement.placed(corner.x(), corner.y());
                Box at = Box.point(turned.x(), turned.y());
                placed = Optional.of(placed.map(box -> box.joined(at)).orElse(at));
            }
        }
        return placed;
    }
}
