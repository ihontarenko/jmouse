package org.jmouse.el.extension;

import org.jmouse.el.extension.filter.LowerFilter;
import org.jmouse.el.extension.filter.UpperFilter;
import org.jmouse.el.extension.function.MaxFunction;
import org.jmouse.el.extension.function.MinFunction;
import org.jmouse.el.extension.function.SetFunction;
import org.jmouse.el.extension.operator.*;
import org.jmouse.el.extension.test.ArrayTest;
import org.jmouse.el.extension.test.EvenTest;
import org.jmouse.el.extension.test.MapTest;
import org.jmouse.el.extension.test.OddTest;
import org.jmouse.el.parser.*;
import org.jmouse.el.parser.sub.ArgumentsParser;
import org.jmouse.el.parser.sub.KeyValueParser;
import org.jmouse.el.parser.sub.ParenthesesParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StandardExtension implements Extension {

    @Override
    public List<Function> getFunctions() {
        return List.of(
                new MinFunction(),
                new MaxFunction(),
                new SetFunction()
        );
    }

    @Override
    public List<Filter> getFilters() {
        return List.of(
                new LowerFilter(),
                new UpperFilter()
        );
    }

    @Override
    public List<Test> getTests() {
        return List.of(
                new EvenTest(),
                new OddTest(),
                new ArrayTest(),
                new MapTest()
        );
    }

    @Override
    public List<Parser> getParsers() {
        return List.of(
                new OperatorParser(),
                new ExpressionParser(),
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

    @Override
    public List<TagParser> getTagParsers() {
        return List.of();
    }

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
