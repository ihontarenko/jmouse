package org.jmouse.json;

import org.jmouse.el.lexer.SyntaxErrorException;
import org.jmouse.el.lexer.Token;

/**
 * 🧾 A text that is not JSON, said where it stops being JSON: what was expected, what stands there instead, at which line and
 * character.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonSyntaxException extends SyntaxErrorException {

    public JsonSyntaxException(String expected, Token found) {
        super("Not JSON: %s expected, '%s' found at line %d, character %d".formatted(
                expected, found.value(), found.lineNumber(), found.offset() + 1));
    }

    public JsonSyntaxException(String why) {
        super("Not JSON: " + why);
    }
}
