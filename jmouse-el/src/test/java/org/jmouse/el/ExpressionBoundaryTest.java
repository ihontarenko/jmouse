package org.jmouse.el;

import org.jmouse.el.extension.CoreExtension;
import org.jmouse.el.extension.Extension;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.el.lexer.DefaultTokenizer;
import org.jmouse.el.lexer.ExpressionRecognizer;
import org.jmouse.el.lexer.ExpressionSplitter;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParserContext;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** A word an extension declares a boundary ends an expression where it could go on, and nowhere else. */
class ExpressionBoundaryTest {

    @Test
    void anExpressionEndsAtABoundary() {
        assertEquals("step", wordAfterExpression("a + b step 2", true));
        assertEquals("in", wordAfterExpression("x in 1 .. 100", true), "a boundary ends even an operator's word");
    }

    @Test
    void aBoundaryAtTheStartOfAnOperandIsAName() {
        assertEquals("<end>", wordAfterExpression("1 + step", true));
    }

    @Test
    void withoutTheExtensionNothingChanges() {
        assertFalse(context(false).isExpressionBoundary("step"));
        assertEquals("<end>", wordAfterExpression("x in [1, 2]", false));
    }

    /** Parses one expression from the start of the text and names the token it stopped at. */
    private static String wordAfterExpression(String text, boolean bounded) {
        ParserContext context = context(bounded);
        TokenCursor cursor = new DefaultLexer(new DefaultTokenizer(new ExpressionSplitter(), new ExpressionRecognizer()))
                .tokenize(new StringSource("boundary", text));
        cursor.consumeIf(BasicToken.T_SOL);
        context.getParser(ExpressionParser.class).parse(cursor, context);
        return !cursor.hasNext() || cursor.isCurrent(BasicToken.T_EOL) ? "<end>" : cursor.current().value();
    }

    private static ParserContext context(boolean bounded) {
        StandardExtensionContainer extensions = new StandardExtensionContainer();
        extensions.importExtension(new CoreExtension());
        if (bounded) {
            extensions.importExtension(new Extension() {
                @Override
                public Set<String> getExpressionBoundaries() {
                    return Set.of("step", "in");
                }
            });
        }
        return new DefaultParserContext(extensions);
    }
}
