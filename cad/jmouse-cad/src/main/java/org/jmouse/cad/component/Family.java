package org.jmouse.cad.component;

import org.jmouse.cad.DocumentHead;
import org.jmouse.cad.Parameter;

import java.util.List;

/**
 * A family its members share — 74HC CMOS logic —, or a library of functions — {@code junctions}, {@code heat} —: its
 * parameters, its definitions for jMF, and a heat law, each empty when it writes none.
 *
 * @param uses the libraries it uses itself, by slug
 */
public record Family(DocumentHead head, List<String> uses, List<Parameter> parameters, List<Equation> formulas,
                     List<Equation> heat) {

    public Family {
        uses = List.copyOf(uses);
        parameters = List.copyOf(parameters);
        formulas = List.copyOf(formulas);
        heat = List.copyOf(heat);
    }
}
