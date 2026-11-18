package org.jmouse.cad.pcb.checks;

import org.jmouse.cad.pcb.drawing.Zone;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jmouse.cad.document.FindingSeverity.WARNING;
import static org.jmouse.cad.pcb.geometry.CopperPieces.key;

/** Zones poured for a net the circuit does not have: copper that joins nothing it was meant to. */
final class ZoneCheck implements PcbCheck {

    @Override
    public List<PcbFinding> findings(CheckContext context) {
        Set<String> circuitNets = context.subject().partNets().values().stream().flatMap(nets -> nets.values().stream()).collect(Collectors.toSet());
        List<PcbFinding> findings = new ArrayList<>();
        List<Zone> zones = context.pcb().zones();
        for (int index = 0; index < zones.size(); index++) {
            Zone zone = zones.get(index);
            if (!circuitNets.contains(zone.net())) {
                findings.add(new PcbFinding(WARNING, "Zone " + (index + 1) + " is poured for " + zone.net() + ", a net the circuit does not have",
                        List.of(key("zone", index))));
            }
        }
        return findings;
    }
}
