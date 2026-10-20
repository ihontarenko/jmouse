package org.jmouse.formula;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A formula that is affine in some variables — {@code 0.85 x + 0.04 y + 1.6} in {@code x} and {@code y} — as its
 * numbers: one coefficient per variable, and the translation.
 *
 * <p>Read from the tree, not the text: each coefficient is the formula's derivative in its variable, which for an
 * affine formula is a number, and the translation is the formula where every variable is zero. So {@code 2 (x + 1)},
 * {@code x/4} and {@code -(y - 3)} are affine as well as the sum they are equal to.</p>
 *
 * @param coefficients one per variable, in the order the variables were given
 * @param translation  the value where every variable is zero
 */
public record AffineForm(List<Double> coefficients, double translation) {

    public AffineForm {
        coefficients = List.copyOf(coefficients);
    }

    /**
     * The formula's coefficients in these variables; empty when it mentions another name, or is not affine in them —
     * a product of two of them, a function of one.
     */
    public static Optional<AffineForm> of(Formula formula, List<String> variables) {
        if (!variables.containsAll(formula.variables()) || formula.mentionsImaginaryUnit()) {
            return Optional.empty();
        }
        List<Double> coefficients = new ArrayList<>(variables.size());
        Map<String, Formula> origin = new LinkedHashMap<>();
        for (String variable : variables) {
            Formula derivative = Differentiation.derivative(formula, variable);
            if (!derivative.variables().isEmpty()) {
                return Optional.empty();
            }
            coefficients.add(RealCompiler.fold(derivative));
            origin.put(variable, Formula.constant(0));
        }
        return Optional.of(new AffineForm(coefficients, RealCompiler.fold(Substitution.substitute(formula, origin))));
    }
}
