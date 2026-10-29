package org.jmouse.gerber.parser;

import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.gerber.GerberParseException;
import org.jmouse.gerber.lexer.GerberTokenKind;
import org.jmouse.gerber.node.EndOfFile;
import org.jmouse.gerber.node.GerberNode;

import java.util.List;
import java.util.function.Predicate;

/**
 * Reads commands into a file or a region until what closes it: each command handed to the parser the
 * {@link ParserContext} finds for it, and one no parser names kept as written.
 */
final class CommandSequence {

    private static final CommandParser OTHER_EXTENDED = new ExtendedCommandParsers.OtherExtendedParser();
    private static final CommandParser OTHER_WORDS    = new WordCommandParsers.OtherWordParser();

    private CommandSequence() {
    }

    /**
     * @param closing the block that closes the sequence, read with it — {@code G37} for a region —; null for a file,
     *                which ends at {@code M02} or at the end of the text
     */
    static void read(TokenCursor cursor, GerberNode parent, ParserContext context, Predicate<List<String>> closing) {
        while (true) {
            if (GerberTokenKind.END_OF_TEXT.matches(cursor.peek())) {
                if (closing != null) {
                    throw new GerberParseException(parent.position(), "a region opened by G36 is never closed by G37");
                }
                return;
            }
            if (closing != null && GerberTokenKind.WORD.matches(cursor.peek()) && closing.test(CommandParser.wordsAhead(cursor))) {
                CommandParser.readWords(cursor);
                return;
            }
            Parser parser = context.getParser(cursor);
            if (parser == null) {
                parser = fallback(cursor);
            }
            parser.parse(cursor, parent, context);
            if (parent.getLast() instanceof EndOfFile) {
                return;
            }
        }
    }

    private static Parser fallback(TokenCursor cursor) {
        if (OTHER_EXTENDED.supports(cursor)) {
            return OTHER_EXTENDED;
        }
        if (OTHER_WORDS.supports(cursor)) {
            return OTHER_WORDS;
        }
        throw WordCommandParsers.unexpected(cursor);
    }
}
