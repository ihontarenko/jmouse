package org.jmouse.spice.expression;

import org.jmouse.el.extension.Extension;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.operator.ComparisonOperator;
import org.jmouse.el.extension.operator.LogicalOperator;
import org.jmouse.el.extension.operator.MathematicOperator;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.FunctionParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.sub.ArgumentsParser;
import org.jmouse.el.parser.sub.ParenthesesParser;
import org.jmouse.formula.el.FormulaOperator;

import java.util.ArrayList;
import java.util.List;

/**
 * What a SPICE expression may contain, in place of jME's {@code CoreExtension}, and nothing else.
 *
 * <ul>
 *   <li>operators — {@code + - * /}, both of SPICE's powers ({@code **} and {@code ^}, the caret a power as in jMF, not
 *       XOR), the comparisons and {@code &&} / {@code ||}; no {@code %}, which ngspice's expressions do not have;</li>
 *   <li>no functions: a SPICE function is read into jMF's by its name, where the expression is translated
 *       ({@link SpiceFormulaTranslator}), so nothing here evaluates;</li>
 *   <li>parsers — jME's expression parser (it reads {@code c ? a : b}), calls, parentheses and arguments; the operator
 *       and primary grammars are SPICE's, handed out by {@link SpiceExtensionContainer} at jME's lookup.</li>
 * </ul>
 */
final class SpiceCoreExtension implements Extension {

    @Override
    public List<Operator> getOperators() {
        List<Operator> operators = new ArrayList<>(List.of(MathematicOperator.PLUS, MathematicOperator.SUBTRACT,
                MathematicOperator.MULTIPLY, MathematicOperator.DIVIDE, MathematicOperator.EXPONENTIAL,
                FormulaOperator.POWER));
        operators.addAll(List.of(ComparisonOperator.values()));
        operators.add(LogicalOperator.AND);
        operators.add(LogicalOperator.OR);
        return operators;
    }

    @Override
    public List<Parser> getParsers() {
        return List.of(new ExpressionParser(), new SpiceOperatorParser(), new SpicePrimaryExpressionParser(),
                new FunctionParser(), new ParenthesesParser(), new ArgumentsParser());
    }
}
