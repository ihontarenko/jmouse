package org.jmouse.template.extension;

import org.jmouse.template.extension.function.MaxFunction;
import org.jmouse.template.extension.function.MinFunction;
import org.jmouse.template.extension.operator.BinaryOperator;
import org.jmouse.template.extension.operator.LogicalOperator;
import org.jmouse.template.extension.operator.UnaryOperator;
import org.jmouse.template.parsing.parser.*;
import org.jmouse.template.parsing.Parser;
import org.jmouse.template.parsing.TagParser;
import org.jmouse.template.parsing.parser.sub.ArgumentsParser;
import org.jmouse.template.parsing.parser.sub.ParenthesesParser;
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

        operators.addAll(Arrays.asList(UnaryOperator.values()));
        operators.addAll(Arrays.asList(LogicalOperator.values()));
        operators.addAll(Arrays.asList(BinaryOperator.values()));

        return operators;
    }
}
