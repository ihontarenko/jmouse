package org.jmouse.cad.simulation;

import org.jmouse.cad.component.Equation;

import java.util.List;

/**
 * How a part warms: its heat law's lines — how fast what it stores changes ({@code d(heating)/dt = …}), what its
 * temperature is ({@code temperature = ambient + heating}) — and the definitions they may call, from the document that
 * writes the law.
 *
 * @param source the document the law is written in: the part's own, a library it uses, the catalogue's shared one
 */
public record HeatLaw(String source, List<Equation> lines, List<Equation> formulas) {

    public HeatLaw {
        lines = List.copyOf(lines);
        formulas = List.copyOf(formulas);
    }
}
