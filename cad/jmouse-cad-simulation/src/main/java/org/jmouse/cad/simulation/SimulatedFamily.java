package org.jmouse.cad.simulation;

import org.jmouse.cad.component.Equation;

import java.util.List;

/**
 * A logic family as the engine runs it: its parameters in SI, and the definitions its laws are — every library it uses,
 * depth first, then its own — that each of its parts reads its thresholds, its output and its delays by.
 */
public record SimulatedFamily(String slug, String title, List<EvaluatedParameter> parameters, List<Equation> formulas) {

    public SimulatedFamily {
        parameters = List.copyOf(parameters);
        formulas = List.copyOf(formulas);
    }
}
