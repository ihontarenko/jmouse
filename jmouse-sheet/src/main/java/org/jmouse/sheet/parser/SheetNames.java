package org.jmouse.sheet.parser;

import org.jmouse.sheet.SheetSyntaxException;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;

/** The name of a field or an entry: a word with neither {@code #} nor {@code /}, which are a link's. */
final class SheetNames {

    private SheetNames() {
    }

    /**
     * @param what what the name is, for a refusal to say
     */
    static String name(TokenCursor cursor, String what) {
        if (!SheetReading.atWord(cursor)) {
            throw new SheetSyntaxException(cursor, what + " is a word");
        }
        Token first = cursor.lookAt(0);
        String name = SheetReading.word(cursor);
        if (name.indexOf('#') >= 0 || name.indexOf('/') >= 0) {
            throw new SheetSyntaxException(cursor, first, what + " has neither '#' nor '/' in it: those are a link's");
        }
        return name;
    }
}
