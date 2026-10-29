package org.jmouse.cad.simulation;

import org.jmouse.cad.Formula;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.circuit.Placement;
import org.jmouse.cad.circuit.Probe;
import org.jmouse.cad.document.DocumentEditing.BlockRedrawing;
import org.jmouse.el.node.Expression;

import java.util.ArrayList;
import java.util.List;

/**
 * What a schematic editor draws of a circuit: its parts, where they and its marks and wires stand, the frame its sheet
 * stands in, and the bench's probes. What it does not draw — its parameters, ports, expectations, how it is run — stays as
 * the text writes it, edited as a form.
 *
 * @param frame    the frame its sheet stands in, by slug, or null
 * @param timeBase the oscilloscope's time a division, or null
 */
public record CircuitSheet(List<Part> parts, List<Placement> layout, String frame, List<Probe> probes, Formula timeBase) {

    public CircuitSheet {
        parts = parts == null ? List.of() : List.copyOf(parts);
        layout = layout == null ? List.of() : List.copyOf(layout);
        probes = probes == null ? List.of() : List.copyOf(probes);
    }

    /** The sheet a circuit draws. */
    public static CircuitSheet of(Circuit circuit) {
        return new CircuitSheet(circuit.parts(), circuit.layout(), circuit.frame(), circuit.probes(), circuit.timeBase());
    }

    /**
     * The blocks a sheet is written into, in the order a circuit writes them, each line known by its key: what a circuit's
     * text becomes when this sheet stands where {@code before} stood.
     *
     * @param before the sheet the text draws now; null when the text does not read as a circuit
     */
    public List<BlockRedrawing> redrawing(CircuitSheet before, CircuitLines lines) {
        return List.of(
                new BlockRedrawing(SimulationVocabulary.PARTS, before == null ? List.of() : before.partLines(lines),
                                   partLines(lines), CircuitLines::keyOf),
                new BlockRedrawing("layout", before == null ? List.of() : before.layoutLines(lines), layoutLines(lines),
                                   CircuitLines::keyOf),
                new BlockRedrawing("probes", before == null ? List.of() : before.probeLines(lines), probeLines(lines),
                                   CircuitLines::keyOf));
    }

    private List<Expression> partLines(CircuitLines lines) {
        return parts.stream().map(part -> (Expression) lines.part(part)).toList();
    }

    private List<Expression> layoutLines(CircuitLines lines) {
        List<Expression> layoutLines = new ArrayList<>();
        if (frame != null) {
            layoutLines.add(lines.frame(frame));
        }
        layout.forEach(placement -> layoutLines.add(lines.placement(placement)));
        return layoutLines;
    }

    private List<Expression> probeLines(CircuitLines lines) {
        List<Expression> probeLines = new ArrayList<>();
        if (timeBase != null) {
            probeLines.add(lines.timeBase(timeBase));
        }
        probes.forEach(probe -> probeLines.add(lines.probe(probe)));
        return probeLines;
    }
}
