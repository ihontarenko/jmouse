package org.jmouse.sheet.parser;

/**
 * The order the statement parsers are asked in. An entry and a field both begin with a word, and only the brace at
 * the end of the line tells them apart, so the entry is asked first; whatever neither reads is refused, last.
 *
 * <p>⚠️ {@link SheetDocumentParser} is absent: it is the root a reader hands the whole cursor to, never chosen for a
 * statement.</p>
 */
public final class SheetParserPriority {

    public static final int ENTRY = Integer.MIN_VALUE + 1000;
    public static final int FIELD = Integer.MIN_VALUE + 1100;
    public static final int UNREADABLE = Integer.MAX_VALUE;

    private SheetParserPriority() {
    }
}
