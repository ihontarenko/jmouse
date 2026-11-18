package org.jmouse.cad.pcb.checks;

import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.Hole;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.drawing.Via;
import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.cad.pcb.geometry.CopperShapes;
import org.jmouse.geometry.index.BoxIndex;
import org.jmouse.geometry.index.BoxTree;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.jmouse.cad.document.FindingSeverity.ERROR;
import static org.jmouse.cad.pcb.geometry.CopperPieces.key;

/** Holes nearer each other than the pcb's drill keeps between them — a part's own holes are its footprint's to keep. */
final class HoleCheck implements PcbCheck.Ruled {

    /** A hole to measure: what it is named by, whose part it is, and its shape. */
    private record Drilled(String key, String label, Optional<String> part, CopperShape shape) {
    }

    @Override
    public List<PcbFinding> findings(CheckContext context, Map<String, Double> values) {
        List<Drilled> holes = drilledOf(context);
        double keep = FindingWords.rule(values, PcbVocabulary.RULE_HOLE_CLEARANCE);
        List<PcbFinding> findings = new ArrayList<>();
        BoxIndex holesNear = BoxTree.of(holes.stream().map(hole -> hole.shape().box()).toList());
        for (int first = 0; first < holes.size(); first++) {
            for (int second : holesNear.near(holes.get(first).shape().box(), keep)) {
                if (second <= first) {
                    continue;
                }
                Drilled one = holes.get(first);
                Drilled other = holes.get(second);
                if ((one.part().isPresent() && one.part().equals(other.part())) || !one.shape().within(other.shape(), keep)) {
                    continue;
                }
                CopperShape.Gap gap = one.shape().gapTo(other.shape());
                if (gap.gap() < keep - context.settings().tolerance()) {
                    String label = Character.toUpperCase(one.label().charAt(0)) + one.label().substring(1);
                    findings.add(new PcbFinding(ERROR, label + " is " + FindingWords.said(gap.gap()) + " from " + other.label() + ": holes keep "
                            + FindingWords.said(keep) + " apart", new ArrayList<>(new LinkedHashSet<>(List.of(one.key(), other.key()))), gap.at()));
                }
            }
        }
        return findings;
    }

    /** Every hole of the pcb: its parts' pads drilled, its vias, its own holes. */
    private static List<Drilled> drilledOf(CheckContext context) {
        PcbDrawing pcb = context.pcb();
        List<Drilled> holes = new ArrayList<>();
        for (int index = 0; index < pcb.placements().size(); index++) {
            Placement placement = pcb.placements().get(index);
            int at = index;
            context.footprintOf(placement).ifPresent(footprint -> footprint.pads().stream().filter(pad -> pad.drillWidth() > 0)
                    .forEach(pad -> holes.add(new Drilled(key("placement", at), "the hole of " + placement.printed() + " pad "
                            + (pad.number().isEmpty() ? "—" : pad.number()), Optional.of(placement.designator()), CopperShapes.hole(placement, pad)))));
        }
        for (int index = 0; index < pcb.vias().size(); index++) {
            Via via = pcb.vias().get(index);
            holes.add(new Drilled(key("via", index), "via " + (index + 1), Optional.empty(), CopperShape.circle(via.x(), via.y(), via.drill() / 2)));
        }
        for (int index = 0; index < pcb.holes().size(); index++) {
            Hole hole = pcb.holes().get(index);
            holes.add(new Drilled(key("hole", index), "hole " + (index + 1), Optional.empty(), CopperShape.circle(hole.x(), hole.y(), hole.drill() / 2)));
        }
        return holes;
    }
}
