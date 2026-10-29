package org.jmouse.cad.simulation;

import org.jmouse.cad.Formula;
import org.jmouse.cad.Parameter;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.units.Quantity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A component's parameters in SI, in the order written: a value written as a quantity — {@code 4.7 kΩ} — is read by the
 * units library; an expression is computed by jMF from the parameters written before it. A text parameter is no number
 * and is left to the texts; a name the engine gives ({@link EngineName}) is refused.
 */
public final class ParameterEvaluation {

    /** What a part line shows beside its part — {@code value = LM358} —: no parameter may take its name. */
    private static final String PART_VALUE = "value";

    /** The unit of a parameter counted per unit of a package: {@code 5 V per unit}. */
    private static final String PER_UNIT = "unit";

    private ParameterEvaluation() {
    }

    public static List<EvaluatedParameter> evaluate(String title, List<Parameter> parameters) {
        List<EvaluatedParameter> evaluated = new ArrayList<>();
        Map<String, Double> known = new LinkedHashMap<>();
        for (Parameter parameter : parameters) {
            if (!(parameter.value() instanceof Formula formula)) {
                continue;
            }
            requireOwnName(title, parameter.name());
            Optional<Quantity> quantity = Quantity.parse(formula.written());
            double value = quantity.map(Quantity::value).orElseGet(() -> computed(title, parameter.name(), formula, known));
            known.put(parameter.name(), value);
            evaluated.add(new EvaluatedParameter(parameter.name(), value, PER_UNIT.equals(parameter.per()),
                    parameter.description(), quantity.map(Quantity::unit).orElse("")));
        }
        return evaluated;
    }

    /**
     * A written value: a quantity — {@code 4.7 kΩ} — as the units library reads it, or an expression jMF computes over
     * the values {@code known}.
     *
     * @param where what a refusal names: the part, the setting
     */
    public static double value(String where, Formula formula, Map<String, Double> known) {
        return Quantity.parse(formula.written()).map(Quantity::value).orElseGet(() -> computed(where, formula, known));
    }

    private static double computed(String where, Formula formula, Map<String, Double> known) {
        try {
            return FormulaLanguage.standard().evaluate(formula.written(), known);
        } catch (RuntimeException refused) {
            throw new IllegalArgumentException(where + " = '" + formula.written() + "' cannot be computed — " + refused.getMessage(), refused);
        }
    }

    /** Whether no parameter may take that name: one the engine gives every component, or what a part line shows. */
    public static boolean reserved(String name) {
        return EngineName.of(name).isPresent() || name.equals(PART_VALUE);
    }

    private static void requireOwnName(String title, String name) {
        if (EngineName.of(name).isPresent()) {
            throw new IllegalArgumentException(title + ": '" + name + "' is a name the engine gives every component's "
                    + "equations; it cannot be a parameter.");
        }
        if (name.equals(PART_VALUE)) {
            throw new IllegalArgumentException(title + ": 'value' is what a part line shows beside its part — "
                    + "'U1 lm358 (…) value = LM358' —, so it cannot be a parameter; name what it holds.");
        }
    }

    private static double computed(String title, String name, Formula formula, Map<String, Double> known) {
        try {
            return FormulaLanguage.standard().evaluate(formula.written(), known);
        } catch (RuntimeException refused) {
            throw new IllegalArgumentException(title + ": the parameter '" + name + "' = '" + formula.written()
                    + "' cannot be computed — " + refused.getMessage(), refused);
        }
    }
}
