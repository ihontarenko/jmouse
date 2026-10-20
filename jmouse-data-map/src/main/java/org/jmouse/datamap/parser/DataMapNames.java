package org.jmouse.datamap.parser;

import org.jmouse.datamap.DataMapSyntaxException;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;

/** The name of a field or an entry: a word with neither {@code #} nor {@code /}, which are a link's. */
final class DataMapNames {

    private DataMapNames() {
    }

    /**
     * @param what what the name is, for a refusal to say
     */
    static String name(TokenCursor cursor, String what) {
        if (!DataMapReading.atWord(cursor)) {
            throw new DataMapSyntaxException(cursor, what + " is a word");
        }
        Token first = cursor.lookAt(0);
        String name = DataMapReading.word(cursor);
        if (name.indexOf('#') >= 0 || name.indexOf('/') >= 0) {
            throw new DataMapSyntaxException(cursor, first, what + " has neither '#' nor '/' in it: those are a link's");
        }
        return name;
    }
}
