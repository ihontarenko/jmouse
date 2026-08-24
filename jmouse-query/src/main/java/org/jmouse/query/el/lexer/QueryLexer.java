package org.jmouse.query.el.lexer;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Cursor;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.lexer.Tokenizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads a {@code .jmq} document, and throws away the two things it has no use for: line breaks and
 * comments.
 *
 * <h2>⚠️ A newline means nothing in this language, and pretending otherwise broke real documents</h2>
 *
 * <p>jMQ is delimited by keywords and braces, not by lines. But the shared lexer emits a
 * {@code T_NEW_LINE} token, and the expression parser stops at any token it cannot continue with — so a
 * condition wrapped over two lines simply <em>ended</em> at the break:</p>
 *
 * <pre>{@code
 * where issue.key is starts('KW-1')
 *       and !(issue.status == 'done')     ← "'and' is not a clause"
 * }</pre>
 *
 * <p>That is not an edge case. A real condition does not fit on one line, and a language that demands
 * it is a language people fight rather than use.</p>
 *
 * <p>⚠️ <strong>Fixed here rather than in the parser, and the reason is precedence.</strong> The
 * tempting fix — noticing the dangling {@code and} and continuing — has to re-join two expressions the
 * parser has already finished, and joining them by hand gets the precedence wrong:
 * {@code a AND b OR c} would be assembled as {@code a AND (b OR c)}. Removing the token instead leaves
 * one expression for one parser to read, and nothing has to be re-derived.</p>
 *
 * <h2>⚠️ Comments go at the same time, and they have to</h2>
 *
 * <p>A {@code #} comment runs to the end of its line. With newlines already gone, a parser stripping
 * comments would have nothing to stop at and would swallow the rest of the document. Here the line
 * number is still on the token, so both are removed in one pass — and the parser no longer has to know
 * comments exist.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryLexer implements Lexer {

    private final Tokenizer<TokenizableSource, Token> tokenizer;

    public QueryLexer(Tokenizer<TokenizableSource, Token> tokenizer) {
        this.tokenizer = tokenizer;
    }

    @Override
    public TokenCursor tokenize(CharSequence text) {
        if (!(text instanceof TokenizableSource source)) {
            return null;
        }

        return new Cursor(source, meaningful(tokenizer.tokenize(source)));
    }

    /**
     * Everything but the line breaks and the commented-out text.
     *
     * <p>⚠️ A {@code #} inside a string literal is already part of that literal's token, so a value like
     * {@code '#4 red'} is not mistaken for a comment. That falls out of the lexer's own ordering rather
     * than being handled here, and it is worth knowing that it does.</p>
     */
    private List<Token> meaningful(List<Token> tokens) {
        List<Token> kept = new ArrayList<>(tokens.size());

        int commentedLine = Integer.MIN_VALUE;

        for (Token token : tokens) {
            if (token.type() == BasicToken.T_HASH) {
                commentedLine = token.lineNumber();
                continue;
            }

            if (token.lineNumber() == commentedLine && token.type() != BasicToken.T_EOL) {
                continue;
            }

            if (token.type() == BasicToken.T_NEW_LINE) {
                continue;
            }

            kept.add(token);
        }

        return names(kept);
    }

    /**
     * Re-reads a keyword as a plain name where it is plainly being used as one.
     *
     * <h2>⚠️ Because a product's data does not know this language's keywords</h2>
     *
     * <p>{@code order}, {@code value}, {@code key}, {@code columns}, {@code group}, {@code source} are all
     * perfectly ordinary things to call a field, and every one of them is a word this grammar spends.
     * Without this, a filter could not say {@code order.number} at all — and refusing would mean a query
     * language that dictates what a product may name its data, which no product would accept.</p>
     *
     * <p>⚠️ <strong>The test is the NEXT token, and it is unambiguous.</strong> A keyword followed by a
     * {@code .} or a {@code [} is being dereferenced, and no clause in this grammar is ever followed by
     * either: {@code order entry[q] asc} has an identifier after the keyword, {@code columns a, b} the
     * same. So the two readings never overlap, and this is not a heuristic that gets it right most of the
     * time.</p>
     *
     * <p>It is done in the lexer rather than in the parser for the same reason newlines are: the parser
     * would have to re-decide it at every site that reads a name, and the one site somebody forgets is
     * the one that refuses a legitimate query.</p>
     */
    private List<Token> names(List<Token> tokens) {
        List<Token> rewritten = new ArrayList<>(tokens.size());

        for (int index = 0; index < tokens.size(); index++) {
            Token token = tokens.get(index);
            Token following = index + 1 < tokens.size() ? tokens.get(index + 1) : null;

            boolean dereferenced = following != null
                                   && (following.type() == BasicToken.T_DOT
                                       || following.type() == BasicToken.T_OPEN_BRACKET);

            if (token.type() instanceof QueryToken && dereferenced) {
                rewritten.add(new Token(token.value(), BasicToken.T_IDENTIFIER,
                        token.ordinal(), token.offset(), token.lineNumber()));
                continue;
            }

            rewritten.add(token);
        }

        return rewritten;
    }
}
