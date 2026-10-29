package org.jmouse.cad.document;

import org.jmouse.el.grammar.ValueKind;
import org.jmouse.el.grammar.ValueReading;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Expression;

import java.util.Set;

/** The kinds of value the jM language brings beside the grammar library's own. */
public final class DocumentValues {

    /** SVG path data in quotes, read into its tree by jmouse-svg-path: {@code "M-4 0H40"}. */
    public static final ValueKind PATH = new ValueKind() {
        @Override
        public boolean standsAt(TokenCursor cursor, Set<String> words, Set<String> allowed) {
            return cursor.isCurrent(BasicToken.T_STRING);
        }

        @Override
        public Expression read(ValueReading reading) {
            TokenCursor cursor = reading.cursor();
            if (!cursor.isCurrent(BasicToken.T_STRING)) {
                throw reading.refusal();
            }
            return DocumentPaths.read(cursor, cursor.next());
        }
    };

    private DocumentValues() {
    }
}
