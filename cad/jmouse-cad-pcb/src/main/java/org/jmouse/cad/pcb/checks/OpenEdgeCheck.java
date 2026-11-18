package org.jmouse.cad.pcb.checks;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.geometry.PathOutlines;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.jmouse.cad.document.FindingSeverity.ERROR;
import static org.jmouse.cad.pcb.geometry.CopperPieces.key;

/** An edge that does not close: there is no telling what is inside it, and no factory can cut it. */
final class OpenEdgeCheck implements PcbCheck {

    /** How many places a point of an open end is said to. */
    private static final int PLACES = 3;

    @Override
    public List<PcbFinding> findings(CheckContext context) {
        List<PcbFinding> findings = new ArrayList<>();
        List<String> outlines = PathOutlines.subpaths(context.pcb().outline());
        for (int index = 0; index < outlines.size(); index++) {
            String outline = outlines.get(index);
            if (PathOutlines.isClosed(outline)) {
                continue;
            }
            List<Point> ends = PathOutlines.openEnds(outline, context.settings().curveSteps());
            if (ends.isEmpty()) {
                continue;
            }
            findings.add(new PcbFinding(ERROR, "The PCB's edge is open between " + ends.stream()
                    .map(point -> FindingWords.written(FindingWords.rounded(point.x(), PLACES)) + ", " + FindingWords.written(FindingWords.rounded(point.y(), PLACES)))
                    .collect(Collectors.joining(" and ")) + " mm: it cannot be cut", List.of(key("outline", index)), ends.getFirst()));
        }
        return findings;
    }
}
