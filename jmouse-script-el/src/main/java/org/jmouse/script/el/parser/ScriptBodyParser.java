package org.jmouse.script.el.parser;

import org.jmouse.el.language.lexer.LanguageToken;
import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.script.el.lexer.ScriptToken;

/**
 * Reads a word-delimited body — everything between a {@code do} or a {@code then} and the word that
 * ends it.
 *
 * <p>The statement reading itself is {@link StatementsParser}, shared with every brace-delimited
 * language on this engine: trivia, the no-progress guard, a trailing comment staying on the line it
 * was written on. Only the delimiters are this dialect's.</p>
 *
 * <h2>⚠️ It opens and closes nothing</h2>
 *
 * <p>The word that opens a body is part of that construction's <em>header</em> — {@code on … do},
 * {@code if … then} — so the block parser consumes it while reading the header, and the word that ends
 * it is consumed by the same parser's {@code closeBlock}. This reader only has to know where to
 * <em>stop</em>.</p>
 *
 * <p>⚠️ <strong>It stops at {@code else} and {@code elseif} as well as at {@code end}, in every body
 * and not only in a branch.</strong> That is deliberate: none of the three can begin a statement, so
 * stopping at any of them costs nothing, and a stray {@code else} inside a loop then gets refused by
 * that loop's {@code ensure(end)} — which names the construction that is unterminated. The alternative,
 * a reader that only knows about {@code end}, hands the stray word to the expression parser and reports
 * it as an unexpected token with no idea what it belongs to.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptBodyParser extends StatementsParser {

    @Override
    protected void open(TokenCursor cursor) {
    }

    @Override
    protected boolean isClosed(TokenCursor cursor) {
        return !cursor.hasNext()
                || cursor.isCurrent(BasicToken.T_EOL)
                || cursor.isCurrent(ScriptToken.T_END)
                || cursor.isCurrent(LanguageToken.T_ELSE, LanguageToken.T_ELSE_IF);
    }

    @Override
    protected void close(TokenCursor cursor) {
    }

}
