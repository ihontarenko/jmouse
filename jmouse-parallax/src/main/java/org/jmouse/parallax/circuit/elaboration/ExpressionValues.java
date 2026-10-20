package org.jmouse.parallax.circuit.elaboration;

import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.Symbols;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.datamap.Quantity;

import java.util.List;
import java.util.Map;

/**
 * What a part's value works out to where it is unfolded into a larger circuit: a written quantity ({@code 10 kΩ}) as
 * it reads, an expression evaluated over the parameters of the document it was written in — which the larger circuit
 * does not have.
 */
final class ExpressionValues {

    private ExpressionValues() {
    }

    static double valueOf(String expression, Map<String, Double> parameters) {
        if (Quantity.isQuantity(expression)) {
            return Quantity.valueOf(expression);
        }
        List<String> names = List.copyOf(parameters.keySet());
        double[] values = names.stream().mapToDouble(parameters::get).toArray();
        return RealCompiler.compile(FormulaLanguage.standard().parse(expression), Symbols.of(List.of(), names), expression)
                .evaluate(values);
    }
}
