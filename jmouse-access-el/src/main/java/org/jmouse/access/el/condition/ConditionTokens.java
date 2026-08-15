package org.jmouse.access.el.condition;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.el.lexer.DefaultTokenizer;
import org.jmouse.el.lexer.ExpressionRecognizer;
import org.jmouse.el.lexer.ExpressionSplitter;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;

import java.util.ArrayList;
import java.util.List;

/**
 * One condition, lexed once, for everybody who has a question about its words.
 *
 * <p>⚠️ <strong>Once is the point.</strong> Compiling a condition asks three separate questions of the
 * same 100-odd characters — {@link ConditionVocabulary} asks whether every word is allowed here,
 * {@link ConditionReading} asks which names it mentions, and the parser asks what it means — and each
 * used to run the lexer itself. Only the parser's run is cached, so the other two were paid in full
 * every time a condition was compiled, for an answer that could not have changed between them.
 *
 * <p>The two readings want different lists and both are taken from one pass: {@link #all(String)}
 * keeps the source as it lexed, because a vocabulary check that dropped tokens would be a vocabulary
 * check with holes in it, and {@link #significant(List)} drops the structural ones, because a reader
 * that looks at its neighbours can only do so where neighbours are adjacent.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
final class ConditionTokens {

    private static final Lexer LEXER = new DefaultLexer(
            new DefaultTokenizer(new ExpressionSplitter(), new ExpressionRecognizer()));

    private ConditionTokens() {
    }

    /**
     * Every token of the condition, exactly as it lexed.
     *
     * @param source the condition, as it was written in the file
     * @return its tokens, structural ones included
     */
    static List<Token> all(String source) {
        TokenCursor cursor = LEXER.tokenize(new StringSource("CONDITION(" + source + ")", source));
        List<Token> tokens = new ArrayList<>();

        while (cursor.hasNext()) {
            tokens.add(cursor.next());
        }

        return tokens;
    }

    /**
     * The same tokens with the structural ones removed, so that lookahead can count on adjacency.
     *
     * @param tokens what {@link #all(String)} returned
     * @return the tokens that carry meaning
     */
    static List<Token> significant(List<Token> tokens) {
        return tokens.stream().filter(token -> !isStructural(token)).toList();
    }

    private static boolean isStructural(Token token) {
        return token.type() == BasicToken.T_NEW_LINE
               || token.type() == BasicToken.T_SOL
               || token.type() == BasicToken.T_EOL;
    }
}
