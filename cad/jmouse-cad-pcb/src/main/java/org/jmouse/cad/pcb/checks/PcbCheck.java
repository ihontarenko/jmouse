package org.jmouse.cad.pcb.checks;

import java.util.List;
import java.util.Map;

/** One of a pcb's checks: what it finds on a pcb, as a pcb house would before it makes it. */
@FunctionalInterface
public interface PcbCheck {

    /** What the check finds, each finding naming what it is about and where. */
    List<PcbFinding> findings(CheckContext context);

    /** A check that measures against the values the pcb is held to: asked only while they are known. */
    @FunctionalInterface
    interface Ruled {

        /** What the check finds against the pcb's values, by rule word. */
        List<PcbFinding> findings(CheckContext context, Map<String, Double> values);
    }

    /** A ruled check as a check: it finds nothing while the pcb's values are not known. */
    static PcbCheck ruled(Ruled check) {
        return context -> context.values().map(values -> check.findings(context, values)).orElse(List.of());
    }
}
