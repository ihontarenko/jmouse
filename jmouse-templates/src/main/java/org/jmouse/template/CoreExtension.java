package org.jmouse.template;

import org.jmouse.template.extension.Extension;
import org.jmouse.template.extension.Function;
import org.jmouse.template.extension.Operator;
import org.jmouse.template.extension.function.MinFunction;
import org.jmouse.template.extension.operator.BinaryOperator;
import org.jmouse.template.extension.operator.LogicalOperator;
import org.jmouse.template.extension.operator.UnaryOperator;
import org.jmouse.template.parsing.parser.*;
import org.jmouse.template.parsing.Parser;
import org.jmouse.template.parsing.TagParser;
import org.jmouse.template.parsing.parser.sub.ArgumentsParser;
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
                new MinFunction()
        );
    }

    @Override
    public List<Parser> getParsers() {
        return List.of(
                new RootParser(),
                new OperatorParser(),
                new ExpressionParser(),
                new FunctionParser(),
                new PropertyParser(),
                new LiteralParser(),
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
