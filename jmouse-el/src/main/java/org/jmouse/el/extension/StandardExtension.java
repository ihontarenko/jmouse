package org.jmouse.el.extension;

import org.jmouse.el.extension.function.MaxFunction;
import org.jmouse.el.extension.function.MinFunction;
import org.jmouse.el.extension.operator.*;
import org.jmouse.el.extension.test.EvenTest;
import org.jmouse.el.extension.test.OddTest;
import org.jmouse.el.parser.*;
import org.jmouse.el.parser.sub.ArgumentsParser;
import org.jmouse.el.parser.sub.ParenthesesParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StandardExtension implements Extension {

    @Override
    public List<Function> getFunctions() {
        return List.of(
                new MinFunction(),
                new MaxFunction()
        );
    }

    @Override
    public List<Test> getTests() {
        return List.of(
                new EvenTest(),
                new OddTest()
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
