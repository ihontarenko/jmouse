package org.jmouse.cad.pcb.checks;

import org.jmouse.cad.document.FindingSeverity;
import org.jmouse.cad.pcb.drawing.PcbDrawing;

import java.util.List;
import java.util.Optional;

/**
 * What a pcb's checks find: how much it weighs, what it says, what it is about — the keys an editor selects it by,
 * {@code track:3} — and where it is, to be marked.
 */
public record PcbFinding(FindingSeverity severity, String message, List<String> keys, Optional<PcbDrawing.Point> at) {

    public PcbFinding {
        keys = List.copyOf(keys);
    }

    public PcbFinding(FindingSeverity severity, String message, List<String> keys) {
        this(severity, message, keys, Optional.empty());
    }

    public PcbFinding(FindingSeverity severity, String message, List<String> keys, PcbDrawing.Point at) {
        this(severity, message, keys, Optional.ofNullable(at));
    }
}
