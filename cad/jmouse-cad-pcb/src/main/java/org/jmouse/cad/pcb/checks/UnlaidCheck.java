package org.jmouse.cad.pcb.checks;

import java.util.List;

import static org.jmouse.cad.document.FindingSeverity.NOTICE;

/** What is not laid yet: the connections the circuit asks for that no copper makes. */
final class UnlaidCheck implements PcbCheck {

    @Override
    public List<PcbFinding> findings(CheckContext context) {
        int unlaid = context.nets().unlaid().size();
        if (unlaid == 0) {
            return List.of();
        }
        return List.of(new PcbFinding(NOTICE, unlaid + (unlaid == 1 ? " connection is" : " connections are") + " not laid yet", List.of()));
    }
}
