package org.jmouse.testing_ground.templates;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.StringSource;
import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Filter;
import org.jmouse.el.extension.StandardExtension;
import org.jmouse.el.lexer.*;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.testing_ground.binder.dto.User;

import java.math.BigDecimal;

public class Expressions {

    public static void main(String[] args) {
        StringSource source = new StringSource("example", "user.name | length('хуй')");

        DefaultTokenizer tokenizer = new DefaultTokenizer(new ExpressionSplitter(), new ExpressionRecognizer());
        Lexer             lexer     = new DefaultLexer(tokenizer);

        TokenCursor cursor = lexer.tokenize(source);
        cursor.next();

        ParserContext parserContext = new DefaultParserContext();
        parserContext.importExtension(new StandardExtension());

        ExpressionNode compiled = (ExpressionNode) parserContext.getParser(ExpressionParser.class).parse(cursor, parserContext);

        EvaluationContext evaluationContext = new DefaultEvaluationContext();
        evaluationContext.getExtensions().importExtension(new StandardExtension());
        evaluationContext.getExtensions().addFilter(new Filter() {
            @Override
            public Object apply(Object input, Arguments arguments, ClassTypeInspector inspector, EvaluationContext context) {
                System.out.println(arguments.getFirst());
                System.out.println(inspector.isBean());
                return ((String) input).length();
            }

            @Override
            public String getName() {
                return "length";
            }
        });

        evaluationContext.setValue("test", 256);

        User user = new User();
        user.setName("ivan");

        evaluationContext.setValue("user", user);

        Object value = compiled.evaluate(evaluationContext);

        System.out.println(value);
    }

}
