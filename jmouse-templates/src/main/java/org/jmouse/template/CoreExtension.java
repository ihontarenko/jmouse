package org.jmouse.template;

import org.jmouse.template.extension.Extension;
import org.jmouse.template.extension.Operator;
import org.jmouse.template.extension.operator.BinaryOperator;
import org.jmouse.template.extension.operator.LogicalOperator;
import org.jmouse.template.extension.operator.UnaryOperator;
import org.jmouse.template.parser.ExpressionParser;
import org.jmouse.template.parser.Parser;
import org.jmouse.template.parser.TagParser;
import org.jmouse.template.parser.global.FunctionParser;
import org.jmouse.template.parser.global.LiteralParser;
import org.jmouse.template.parser.global.OperatorParser;
import org.jmouse.template.parser.global.RootParser;
import org.jmouse.template.parser.tag.ForParser;
import org.jmouse.template.parser.tag.IfParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CoreExtension implements Extension {

    @Override
    public List<Parser> getParsers() {
        return List.of(
                new RootParser(),
                new OperatorParser(),
                new ExpressionParser(),
                new FunctionParser(),
                new LiteralParser()
        );
    }

    @Override
    public List<TagParser> getTagParsers() {
        return List.of(
                new ForParser(),
                new IfParser()
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
