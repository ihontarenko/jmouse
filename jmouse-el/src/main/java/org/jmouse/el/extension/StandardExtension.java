package org.jmouse.el.extension;

import org.jmouse.el.extension.filter.*;
import org.jmouse.el.extension.function.MaxFunction;
import org.jmouse.el.extension.function.MinFunction;
import org.jmouse.el.extension.function.SetFunction;
import org.jmouse.el.extension.operator.*;
import org.jmouse.el.extension.test.*;
import org.jmouse.el.parser.*;
import org.jmouse.el.parser.sub.ArgumentsParser;
import org.jmouse.el.parser.sub.KeyValueParser;
import org.jmouse.el.parser.sub.ParenthesesParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A standard implementation of the {@link Extension} interface.
 * <p>
 * This class aggregates functions, filters, tests, parsers, tag parsers, and operators
 * used in the expression language.
 * </p>
 */
public class StandardExtension implements Extension {

    /**
     * Returns the list of functions available in the expression language.
     *
     * @return a list of {@link Function} instances
     */
    @Override
    public List<Function> getFunctions() {
        return List.of(
                new MinFunction(),
                new MaxFunction(),
                new SetFunction()
        );
    }

    /**
     * Returns the list of filters available in the expression language.
     *
     * @return a list of {@link Filter} instances
     */
    @Override
    public List<Filter> getFilters() {
        return List.of(
                new LowerFilter(),
                new UpperFilter(),
                new ToBigDecimalFilter(),
                new SubFilter(),
                new DefaultFilter(),
                new TrimFilter()
        );
    }

    /**
     * Returns the list of tests available in the expression language.
     *
     * @return a list of {@link Test} instances
     */
    @Override
    public List<Test> getTests() {
        return List.of(
                new EvenTest(),
                new OddTest(),
                new ArrayTest(),
                new MapTest(),
                new IterableTest(),
                new InsetTest()
        );
    }

    /**
     * Returns the list of parsers used for parsing expressions.
     *
     * @return a list of {@link Parser} instances
     */
    @Override
    public List<Parser> getParsers() {
        return List.of(
                new ExpressionParser(),
                new OperatorParser(),
                new PrimaryExpressionParser(),
                new FunctionParser(),
                new TestParser(),
                new FilterParser(),
                new PropertyParser(),
                new LiteralParser(),
                new ArrayParser(),
                new MapParser(),
                new KeyValueParser(),
                new ParenthesesParser(),
                new ArgumentsParser()
        );
    }

    /**
     * Returns the list of tag parsers used in the expression language.
     *
     * @return a list of {@link TagParser} instances, or an empty list if none are defined
     */
    @Override
    public List<TagParser> getTagParsers() {
        return List.of();
    }

    /**
     * Returns the list of operators available in the expression language.
     *
     * @return a list of {@link Operator} instances
     */
    @Override
    public List<Operator> getOperators() {
        List<Operator> operators = new ArrayList<>();

        operators.addAll(Arrays.asList(MathematicOperator.values()));
        operators.addAll(Arrays.asList(UnaryOperator.values()));
        operators.addAll(Arrays.asList(LogicalOperator.values()));
        operators.addAll(Arrays.asList(ComparisonOperator.values()));
        operators.addAll(Arrays.asList(TestOperator.values()));

        return operators;
    }
}
