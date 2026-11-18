package org.jmouse.cad.pcb.checks;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.Lettering;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.cad.pcb.geometry.PlacedDesignator;
import org.jmouse.geometry.index.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.jmouse.cad.document.FindingSeverity.WARNING;
import static org.jmouse.cad.pcb.geometry.CopperPieces.key;

/**
 * Silkscreen a pcb house would print cut: a designator or a text lying over a pad on its own side or over a mounting hole — no ink is
 * printed on bare copper or into a hole —, or standing beyond the pcb's edge, where it is milled away. One finding a marking, naming
 * the first thing it covers.
 */
final class SilkscreenCheck implements PcbCheck {

    /** What a side's silkscreen layer ends with after its side: {@code top-silkscreen}. */
    private static final String SILKSCREEN_ENDING = PcbVocabulary.LAYER_JOINER + PcbVocabulary.SILKSCREEN;

    /** A marking that may be printed cut: what it is named by and as, its side, the box it covers and its corners. */
    private record Marking(String key, String label, String side, Box box, List<Point> corners) {
    }

    @Override
    public List<PcbFinding> findings(CheckContext context) {
        PcbDrawing pcb = context.pcb();
        Optional<CopperShape> edge = CopperShape.ofPath(pcb.outline(), true, 0, context.settings().curveSteps());
        List<PcbFinding> findings = new ArrayList<>();
        for (Marking marking : markingsOf(context)) {
            Point at = CheckShapes.middleOf(marking.box());
            Optional<CopperNets.PlacedPad> pad = context.nets().pads().stream()
                    .filter(candidate -> (candidate.pad().isPlatedThrough() || candidate.pad().hasCopperOn(marking.side(), candidate.placement()))
                                         && marking.box().overlaps(CheckShapes.padBox(candidate)))
                    .findFirst();
            if (pad.isPresent()) {
                findings.add(new PcbFinding(WARNING, marking.label() + " lies over " + pad.get().placement().printed() + " pad "
                        + pad.get().pad().number() + ": it would be printed cut", List.of(marking.key()), at));
                continue;
            }
            Optional<Integer> hole = FindingWords.firstIndexWhere(pcb.holes(), candidate -> marking.box().overlaps(CheckShapes.holeBox(candidate)));
            if (hole.isPresent()) {
                findings.add(new PcbFinding(WARNING, marking.label() + " lies over hole " + (hole.get() + 1) + ": it would be printed cut",
                        List.of(marking.key(), key("hole", hole.get())), at));
                continue;
            }
            if (edge.isPresent() && marking.corners().stream().anyMatch(corner -> !edge.get().encloses(corner.x(), corner.y()))) {
                findings.add(new PcbFinding(WARNING, marking.label() + " runs beyond the PCB's edge: it would be milled away", List.of(marking.key()), at));
            }
        }
        return findings;
    }

    /** What the pcb prints on its silkscreen: each part's designator where it is printed, and each text on a silkscreen layer. */
    private static List<Marking> markingsOf(CheckContext context) {
        PcbDrawing pcb = context.pcb();
        PlacedDesignator.Rules designator = context.settings().designator();
        List<Marking> markings = new ArrayList<>();
        for (int index = 0; index < pcb.placements().size(); index++) {
            Placement placement = pcb.placements().get(index);
            PlacedDesignator standing = PlacedDesignator.of(placement, context.footprintOf(placement), designator);
            if (!standing.printed()) {
                continue;
            }
            Box box = standing.bounds(placement.designator(), designator);
            markings.add(new Marking(key("designator", index), placement.printed() + "'s designator", placement.side(), box, CheckShapes.cornersOf(box)));
        }
        int textIndex = 0;
        for (Lettering text : pcb.texts()) {
            if (text.layer().endsWith(SILKSCREEN_ENDING)) {
                List<Point> corners = context.textCorners(text);
                markings.add(new Marking(key("text", textIndex), "Text \"" + text.text() + "\"",
                        text.layer().substring(0, text.layer().length() - SILKSCREEN_ENDING.length()), CheckShapes.boxOf(corners), corners));
            }
            textIndex++;
        }
        return markings;
    }
}
