package org.jmouse.mapper.el.parser;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.support.SourceReading;
import org.jmouse.mapper.el.lexer.JmmToken;

/**
 * Reads a Java type name as the file writes it.
 *
 * <h2>⚠️ A dotted name is not enough, because a DTO is usually a nested class</h2>
 *
 * <p>{@code shop.api.OrderRequest} joins on dots and a general dotted-name reader handles it. But
 * {@code Checkout$OrderRequest} is what a nested class is called, and the lexer gives {@code Checkout},
 * {@code $}, {@code OrderRequest} — so a dotted reader stops at {@code Checkout} and leaves the rest
 * behind, where it fails as though the next line were malformed. Nested classes as request and response
 * types are ordinary rather than exotic, and a language that cannot name them is a language people work
 * around.</p>
 *
 * <p>⚠️ The run cannot swallow what follows: nothing legal after a type name — a brace, a colon, a
 * newline — begins with a dot or a dollar.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class TypeNames {

    private TypeNames() {
    }

    /**
     * Reads a type name from the cursor's position.
     *
     * @param cursor the cursor, positioned on the first token of the name
     * @return the name, dots and dollars included
     */
    public static String read(TokenCursor cursor) {
        if (cursor.isCurrent(BasicToken.T_STRING)) {
            return SourceReading.literal(cursor.ensure(BasicToken.T_STRING));
        }

        StringBuilder name = new StringBuilder(cursor.ensure(JmmToken.nameTokens()).value());

        while (cursor.isCurrent(BasicToken.T_DOT, BasicToken.T_DOLLAR)
                && cursor.isNext(JmmToken.nameTokens())) {
            name.append(cursor.isCurrent(BasicToken.T_DOT) ? '.' : '$');
            cursor.next();
            name.append(cursor.ensure(JmmToken.nameTokens()).value());
        }

        return name.toString();
    }
}
