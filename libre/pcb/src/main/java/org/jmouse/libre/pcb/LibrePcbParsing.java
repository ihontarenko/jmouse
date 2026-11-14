package org.jmouse.libre.pcb;

import org.jmouse.sexpression.SExpressionParser;
import org.jmouse.sexpression.node.SExpressionList;

/** A LibrePCB file read as its lists: what a project and a library are both read through. */
final class LibrePcbParsing {

    private static final SExpressionParser PARSER = new SExpressionParser();

    private LibrePcbParsing() {
    }

    /** @throws LibrePcbReadException naming the file that does not read */
    static SExpressionList parsed(String path, String text) {
        try {
            return PARSER.parse(path, text).root();
        } catch (IllegalArgumentException unread) {
            throw new LibrePcbReadException(path + " does not read: " + unread.getMessage(), unread);
        }
    }
}
