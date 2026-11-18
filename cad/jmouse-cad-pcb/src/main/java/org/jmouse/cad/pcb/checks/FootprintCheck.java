package org.jmouse.cad.pcb.checks;

import org.jmouse.cad.pcb.drawing.PcbDrawing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.jmouse.cad.document.FindingSeverity.ERROR;
import static org.jmouse.cad.document.FindingSeverity.WARNING;

/** Parts with no footprint chosen, and footprints the library does not have. */
final class FootprintCheck implements PcbCheck {

    /** How many bare parts a finding names; the rest it counts. */
    private static final int NAMED_BARE = 6;

    @Override
    public List<PcbFinding> findings(CheckContext context) {
        PcbDrawing pcb = context.pcb();
        List<PcbFinding> findings = new ArrayList<>();
        List<Integer> bare = new ArrayList<>();
        List<Integer> missing = new ArrayList<>();
        for (int index = 0; index < pcb.placements().size(); index++) {
            String designator = pcb.placements().get(index).designator();
            Optional<String> named = context.subject().named().getOrDefault(designator, Optional.empty());
            if (named.isEmpty()) {
                bare.add(index);
            } else if (!context.subject().footprints().containsKey(designator)) {
                missing.add(index);
            }
        }
        if (!bare.isEmpty()) {
            String shown = bare.stream().limit(NAMED_BARE).map(context::printed).collect(Collectors.joining(", "));
            findings.add(new PcbFinding(ERROR, shown + (bare.size() > NAMED_BARE ? " and " + (bare.size() - NAMED_BARE) + " more" : "")
                    + (bare.size() == 1 ? " has" : " have") + " no footprint: choose one", FindingWords.keys("placement", bare)));
        }
        if (!missing.isEmpty()) {
            findings.add(new PcbFinding(WARNING, "The footprint of " + missing.stream().map(context::printed).collect(Collectors.joining(", "))
                    + " is not in the library", FindingWords.keys("placement", missing)));
        }
        return findings;
    }
}
