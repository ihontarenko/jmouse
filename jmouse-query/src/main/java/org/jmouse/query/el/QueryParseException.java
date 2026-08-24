package org.jmouse.query.el;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.parser.ParseException;
import org.jmouse.query.el.lexer.QueryToken;

/**
 * A {@code .jmq} document that cannot be read, and why.
 *
 * <p>⚠️ <strong>Every message here names the fix, not only the fault.</strong> A query language is
 * written by people who are not writing code, often in a text box, and a refusal is the entire teaching
 * surface the language has. "Unexpected token" tells somebody they were wrong; it does not tell them
 * what to type.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class QueryParseException extends ParseException {

    public QueryParseException(String message) {
        super(message);
    }

    /**
     * A word the language recognises but does not yet accept.
     *
     * <p>The distinct message matters: "reserved for a later version" and "unknown word" send a reader
     * to two different places, and a reserved word looks exactly like a typo without it.</p>
     *
     * @param keyword the reserved word that was written
     * @param token   where it was written
     * @return the refusal to throw
     */
    public static QueryParseException reserved(QueryToken keyword, Token token) {
        return new QueryParseException(
                ("'%s' at line %d is reserved for a later version of jMQ and cannot be used yet; "
                 + "if it was meant as a name, quote it")
                        .formatted(keyword.spelling(), token.lineNumber()));
    }

    /**
     * A clause written twice in one block.
     *
     * <p>⚠️ Refused rather than resolved. Two {@code where} lines read as though they ought to be
     * {@code and}-ed together, and quietly keeping the last one is the kind of thing nobody notices
     * until a view returns rows it should not.</p>
     *
     * @param clause the clause keyword
     * @param token  where the second one was written
     * @return the refusal to throw
     */
    public static QueryParseException repeated(String clause, Token token) {
        return new QueryParseException(
                ("'%s' at line %d is the second one in this block, and a clause may appear once; "
                 + "join the two conditions with 'and' instead")
                        .formatted(clause, token.lineNumber()));
    }

    /**
     * Something that is not a clause, inside a block that holds only clauses.
     *
     * @param token    what was written
     * @param expected what could have been written instead
     * @return the refusal to throw
     */
    public static QueryParseException notAClause(Token token, String expected) {
        return new QueryParseException(
                "'%s' at line %d is not a clause; a block holds %s".formatted(
                        token.value(), token.lineNumber(), expected));
    }
}
