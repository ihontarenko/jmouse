package org.jmouse.el;

import org.jmouse.el.extension.CoreExtension;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.BoundedTokenCursor;
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

/** A parser reading through a bounded view stops where the view ends, and leaves the cursor there. */
class BoundedTokenCursorTest {

    private static final Set<String> WORDS = Set.of("step", "log");

    @Test
    void anExpressionEndsWhereTheViewDoes() {
        assertEquals("step", wordAfterExpression("a + b step 2"));
        assertEquals("log", wordAfterExpression("max(a, b) * 2 log"));
    }

    @Test
    void aWordInsideParenthesesOrCalledIsNoBoundary() {
        assertEquals("<end>", wordAfterExpression("max(step, 2) + 1"));
        assertEquals("<end>", wordAfterExpression("1 + log(2)"));
    }

    /** Parses one expression through a view ending at a word of {@link #WORDS} outside parentheses, not called. */
    private static String wordAfterExpression(String text) {
        StandardExtensionContainer extensions = new StandardExtensionContainer();
        extensions.importExtension(new CoreExtension());
        ParserContext context = new DefaultParserContext(extensions);
        TokenCursor cursor = new DefaultLexer(new DefaultTokenizer(new ExpressionSplitter(), new ExpressionRecognizer()))
                .tokenize(new StringSource("bounded", text));
        cursor.consumeIf(BasicToken.T_SOL);
        TokenCursor view = new BoundedTokenCursor(cursor, (token, next, depth) -> depth == 0
                && WORDS.contains(token.value()) && next.type() != BasicToken.T_OPEN_PAREN);
        context.getParser(ExpressionParser.class).parse(view, context);
        return !cursor.hasNext() || cursor.isCurrent(BasicToken.T_EOL) ? "<end>" : cursor.current().value();
    }
}
