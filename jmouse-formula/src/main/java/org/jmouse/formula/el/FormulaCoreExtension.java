package org.jmouse.formula.el;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Extension;
import org.jmouse.el.extension.Function;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.operator.ComparisonOperator;
import org.jmouse.el.extension.operator.LogicalOperator;
import org.jmouse.el.extension.operator.MathematicOperator;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.FunctionParser;
import org.jmouse.el.parser.LiteralParser;
import org.jmouse.el.parser.OperatorParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.PrimaryExpressionParser;
import org.jmouse.el.parser.PropertyParser;
import org.jmouse.el.parser.sub.ArgumentsParser;
import org.jmouse.el.parser.sub.ParenthesesParser;
import org.jmouse.formula.MathematicalFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * jMF's vocabulary, in place of jME's {@code CoreExtension}: what a formula may contain, and nothing else.
 *
 * <ul>
 *   <li>operators — jME's arithmetic, comparison and {@code and}/{@code or} ones, plus {@code ^} as a power in
 *       place of XOR; no filter, test, range or concatenation is registered, so jME's parser simply does not know
 *       them inside a formula;</li>
 *   <li>functions — every {@link MathematicalFunction}, so jME's own evaluator computes a formula too;</li>
 *   <li>parsers — jME's, limited to literals, names, calls and parentheses. jME's operator parser is registered
 *       as usual; the formula container hands out the formula grammar in its place.</li>
 * </ul>
 */
public class FormulaCoreExtension implements Extension {

    @Override
    public List<Operator> getOperators() {
        List<Operator> operators = new ArrayList<>(Arrays.asList(MathematicOperator.values()));
        operators.add(FormulaOperator.POWER);
        operators.addAll(Arrays.asList(ComparisonOperator.values()));
        operators.add(LogicalOperator.AND);
        operators.add(LogicalOperator.OR);
        return operators;
    }

    @Override
    public List<Function> getFunctions() {
        return mathematicalFunctions();
    }

    /**
     * Every {@link MathematicalFunction} as a jME function — the mathematical vocabulary a dialect with its own
     * grammar takes from jMF without taking the formula grammar (jMS).
     */
    public static List<Function> mathematicalFunctions() {
        return Arrays.stream(MathematicalFunction.values()).<Function>map(FormulaFunction::new).toList();
    }

    @Override
    public List<Parser> getParsers() {
        return List.of(
                new ExpressionParser(),
                new OperatorParser(),
                new FormulaPrimaryExpressionParser(),
                new FunctionParser(),
                new PropertyParser(),
                new LiteralParser(),
                new ParenthesesParser(),
                new ArgumentsParser()
        );
    }

    /** One {@link MathematicalFunction} seen through jME's function contract; its real form is what jME evaluates. */
    public static final class FormulaFunction implements Function {

        private final MathematicalFunction function;

        FormulaFunction(MathematicalFunction function) {
            this.function = function;
        }

        @Override
        public Object execute(Arguments arguments, EvaluationContext context) {
            double[] values = new double[function.arity()];
            for (int index = 0; index < values.length; index++) {
                values[index] = context.getConversion().convert(arguments.get(index), Double.class);
            }
            return function.real().apply(values);
        }

        @Override
        public String getName() {
            return function.symbol();
        }
    }
}
