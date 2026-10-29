package org.jmouse.svg.path.parser;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.AbstractParser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.svg.path.lexer.PathTokenKind;
import org.jmouse.svg.path.node.PathCommand;
import org.jmouse.svg.path.node.PathCommandKind;
import org.jmouse.svg.path.node.PathNumber;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads a command — its letter and as many numbers as its kind takes ({@link PathCommandKind#arity()}) —, then every
 * further group of numbers after it, each one more command of the kind it repeats as.
 */
public final class PathCommandParser extends AbstractParser {

    @Override
    public boolean supports(TokenCursor cursor) {
        return PathTokenKind.COMMAND.matches(cursor.peek());
    }

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        Token letter = cursor.next();
        PathCommandKind kind = PathCommandKind.of(letter.value().charAt(0)).orElseThrow();
        boolean relative = Character.isLowerCase(letter.value().charAt(0));
        parent.add(new PathCommand(kind, relative, true, numbers(cursor, kind, relative, letter)));
        PathCommandKind repeated = kind.repeatedAs();
        while (kind.arity() > 0 && PathTokenKind.NUMBER.matches(cursor.peek())) {
            parent.add(new PathCommand(repeated, relative, false, numbers(cursor, repeated, relative, letter)));
        }
    }

    /** Whether nothing — no space, no comma — stands between a number and the token before it. */
    private static boolean joined(Token before, Token token) {
        return before != null && before.offset() + before.value().length() == token.offset()
                && PathTokenKind.NUMBER.matches(before);
    }

    private static List<PathNumber> numbers(TokenCursor cursor, PathCommandKind kind, boolean relative, Token letter) {
        List<PathNumber> numbers = new ArrayList<>(kind.arity());
        for (int index = 0; index < kind.arity(); index++) {
            Token token = cursor.peek();
            if (!PathTokenKind.NUMBER.matches(token)) {
                throw new PathParseException(cursor.source().getName(), token.offset(), "'" + kind.letter(relative) + "' takes "
                        + kind.arity() + " numbers; " + (index == 0 ? "none follows it" : "only " + index + " follow it"));
            }
            Token before = cursor.lookAt(-1);
            cursor.next();
            numbers.add(new PathNumber(Double.parseDouble(token.value()), token.value(), joined(before, token)));
        }
        return numbers;
    }
}
