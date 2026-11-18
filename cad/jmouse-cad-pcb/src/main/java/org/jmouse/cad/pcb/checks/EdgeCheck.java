package org.jmouse.cad.pcb.checks;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.geometry.CopperPieces;
import org.jmouse.cad.pcb.geometry.CopperShape;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.jmouse.cad.document.FindingSeverity.ERROR;
import static org.jmouse.cad.document.FindingSeverity.WARNING;

/** Copper nearer the pcb's edge than it may come, or beyond the edge altogether. */
final class EdgeCheck implements PcbCheck.Ruled {

    @Override
    public List<PcbFinding> findings(CheckContext context, Map<String, Double> values) {
        Optional<CopperShape> edge = CopperShape.ofPath(context.pcb().outline(), true, 0, context.settings().curveSteps());
        if (edge.isEmpty()) {
            return context.pcb().placements().isEmpty() ? List.of() : List.of(new PcbFinding(WARNING, "The PCB has no edge", List.of()));
        }
        CopperShape line = edge.get().lineOnly();
        double keep = FindingWords.rule(values, PcbVocabulary.RULE_EDGE_CLEARANCE);
        Set<String> outside = new LinkedHashSet<>();
        Set<String> near = new LinkedHashSet<>();
        Point nearAt = null;
        for (CopperPieces.Piece piece : context.pieces()) {
            double[] first = piece.shape().pieces().getFirst();
            if (!edge.get().encloses(first[0], first[1])) {
                outside.add(piece.key());
                continue;
            }
            if (!piece.shape().within(line, keep)) {
                continue;
            }
            CopperShape.Gap gap = piece.shape().gapTo(line);
            if (gap.gap() < keep - context.settings().tolerance()) {
                near.add(piece.key());
                nearAt = nearAt == null ? gap.at() : nearAt;
            }
        }
        List<PcbFinding> findings = new ArrayList<>();
        if (!outside.isEmpty()) {
            findings.add(new PcbFinding(ERROR, (outside.size() == 1 ? "A piece of copper stands" : outside.size() + " pieces of copper stand")
                    + " beyond the PCB's edge", List.copyOf(outside)));
        }
        if (!near.isEmpty()) {
            findings.add(new PcbFinding(ERROR, (near.size() == 1 ? "A piece of copper comes" : near.size() + " pieces of copper come")
                    + " nearer the edge than " + FindingWords.said(keep), List.copyOf(near), nearAt));
        }
        return findings;
    }
}
