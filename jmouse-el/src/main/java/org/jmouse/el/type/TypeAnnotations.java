package org.jmouse.el.type;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParseException;
import org.jmouse.el.parser.ParserContext;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reads the annotation a declaration may carry — {@code : int} after a name — and resolves it against the types the
 * language registered, while the text is parsed: a misspelt type is refused at its line, never discovered at run time.
 */
public final class TypeAnnotations {

    private static final Pattern WORD = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private TypeAnnotations() {
    }

    /**
     * The type after a colon at the cursor, consumed with it; empty, and nothing consumed, where no colon follows.
     *
     * @throws ParseException when the colon is followed by no name, or by a name no extension registered as a type
     */
    public static Optional<ValueType> read(TokenCursor cursor, ParserContext context) {
        if (!cursor.consumeIf(BasicToken.T_COLON)) {
            return Optional.empty();
        }
        Token name = cursor.current();
        if (name == null || !WORD.matcher(name.value()).matches()) {
            throw new ParseException("A type name is expected after ':' at " + cursor.position());
        }
        ValueType type = context.getExtensionContainer().getType(name.value());
        if (type == null) {
            throw new ParseException("'" + name.value() + "' is not a type here. Known types: "
                    + String.join(", ", context.getExtensionContainer().getTypes().stream().map(ValueType::name).sorted()
                    .toList()) + ".");
        }
        cursor.consumeIf(name.type());
        return Optional.of(type);
    }
}
