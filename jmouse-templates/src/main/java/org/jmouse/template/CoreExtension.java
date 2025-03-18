package org.jmouse.template;

import org.jmouse.el.extension.Extension;
import org.jmouse.el.extension.Function;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.function.MaxFunction;
import org.jmouse.el.extension.function.MinFunction;
import org.jmouse.el.extension.operator.*;
import org.jmouse.el.parser.*;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.parser.sub.ArgumentsParser;
import org.jmouse.el.parser.sub.ParenthesesParser;
import org.jmouse.template.parsing.RootParser;
import org.jmouse.template.parsing.tag.ForParser;
import org.jmouse.template.parsing.tag.IfParser;
import org.jmouse.template.parsing.tag.LoremParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CoreExtension implements Extension {

    @Override
    public List<Function> getFunctions() {
        return List.of(
                new MinFunction(),
                new MaxFunction()
        );
    }

    @Override
    public List<Parser> getParsers() {
        return List.of(
                new RootParser(),
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
        return List.of(
                new ForParser(),
                new IfParser(),
                new LoremParser()
        );
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
