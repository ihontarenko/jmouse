package org.jmouse.testing_ground.templates;

import org.jmouse.el.StringSource;
import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.StandardExtension;
import org.jmouse.el.lexer.*;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;

import java.math.BigDecimal;

public class Expressions {

    public static void main(String[] args) {
        StringSource source = new StringSource("example", "test|toBigDecimal**3");

        DefaultTokenizer tokenizer = new DefaultTokenizer(new ExpressionSplitter(), new ExpressionRecognizer());
        Lexer             lexer     = new DefaultLexer(tokenizer);

        TokenCursor cursor = lexer.tokenize(source);
        cursor.next();

        ParserContext parserContext = new DefaultParserContext();
        parserContext.importExtension(new StandardExtension());

        ExpressionNode compiled = (ExpressionNode) parserContext.getParser(ExpressionParser.class).parse(cursor, parserContext);

        EvaluationContext evaluationContext = new DefaultEvaluationContext();
        evaluationContext.getExtensions().importExtension(new StandardExtension());

        evaluationContext.setValue("test", 256);

        Object value = compiled.evaluate(evaluationContext);

        BigDecimal result = evaluationContext.getConversion().convert(value, BigDecimal.class);

        System.out.println(result);
    }

}
