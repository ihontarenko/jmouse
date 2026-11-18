package org.jmouse.cad.pcb.checks;

import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.PcbDrawing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jmouse.cad.document.FindingSeverity.ERROR;
import static org.jmouse.cad.document.FindingSeverity.WARNING;

/** Tracks thinner, vias smaller and rings thinner than the pcb is made with; holes for leads smaller than it drills. */
final class SizeCheck implements PcbCheck.Ruled {

    @Override
    public List<PcbFinding> findings(CheckContext context, Map<String, Double> values) {
        PcbDrawing pcb = context.pcb();
        double tolerance = context.settings().tolerance();
        List<PcbFinding> findings = new ArrayList<>();
        double leastTrack = FindingWords.rule(values, PcbVocabulary.RULE_LEAST_TRACK_WIDTH);
        List<Integer> thin = FindingWords.indexesWhere(pcb.tracks(), track -> track.width() < leastTrack - tolerance);
        if (!thin.isEmpty()) {
            findings.add(new PcbFinding(ERROR, (thin.size() == 1 ? "Track " + (thin.getFirst() + 1) + " is" : thin.size() + " tracks are")
                    + " thinner than the " + FindingWords.said(leastTrack) + " the PCB is made with", FindingWords.keys("track", thin)));
        }
        double leastDrill = FindingWords.rule(values, PcbVocabulary.RULE_LEAST_VIA_DRILL);
        List<Integer> smallDrill = FindingWords.indexesWhere(pcb.vias(), via -> via.drill() < leastDrill - tolerance);
        if (!smallDrill.isEmpty()) {
            findings.add(new PcbFinding(ERROR, (smallDrill.size() == 1 ? "Via " + (smallDrill.getFirst() + 1) + " is" : smallDrill.size() + " vias are")
                    + " drilled smaller than " + FindingWords.said(leastDrill), FindingWords.keys("via", smallDrill)));
        }
        double leastRing = FindingWords.rule(values, PcbVocabulary.RULE_LEAST_VIA_RING);
        List<Integer> thinRing = FindingWords.indexesWhere(pcb.vias(), via -> (via.diameter() - via.drill()) / 2 < leastRing - tolerance);
        if (!thinRing.isEmpty()) {
            findings.add(new PcbFinding(ERROR, (thinRing.size() == 1 ? "Via " + (thinRing.getFirst() + 1) + " has" : thinRing.size() + " vias have")
                    + " a ring thinner than " + FindingWords.said(leastRing), FindingWords.keys("via", thinRing)));
        }
        double leastHole = FindingWords.rule(values, PcbVocabulary.RULE_LEAST_DRILL);
        List<Integer> smallHoles = FindingWords.indexesWhere(pcb.placements(), placement -> context.footprintOf(placement)
                .map(footprint -> footprint.pads().stream().anyMatch(pad -> pad.drillWidth() > 0 && pad.drillWidth() < leastHole - tolerance))
                .orElse(false));
        if (!smallHoles.isEmpty()) {
            findings.add(new PcbFinding(WARNING, smallHoles.stream().map(context::printed).collect(Collectors.joining(", "))
                    + (smallHoles.size() == 1 ? " has" : " have") + " holes smaller than the " + FindingWords.said(leastHole) + " the PCB is drilled with",
                    FindingWords.keys("placement", smallHoles)));
        }
        return findings;
    }
}
