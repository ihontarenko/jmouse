package org.jmouse.gerber.parser;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.gerber.GerberParseException;
import org.jmouse.gerber.GerberPosition;
import org.jmouse.gerber.lexer.GerberTokenKind;
import org.jmouse.gerber.node.GerberCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * A jME {@link Parser} for one kind of Gerber command: it says which commands are its, and builds the node of one.
 * Every command parser is registered in the {@link ParserContext}, which picks the one that {@link #supports supports}
 * the command at the cursor — so a new kind of command is a new parser, not a new branch.
 */
abstract class CommandParser implements Parser {

    /** The command at the cursor, read; the cursor stands after it. */
    abstract GerberCommand command(TokenCursor cursor, ParserContext context);

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        parent.add(command(cursor, context));
    }

    /** Where the token at the cursor stands. */
    static GerberPosition position(TokenCursor cursor) {
        return GerberPosition.of(cursor.peek(), cursor.source());
    }

    /** The token at the cursor, which must be of this kind. */
    static Token expect(TokenCursor cursor, GerberTokenKind kind, String shape) {
        Token token = cursor.peek();
        if (!kind.matches(token)) {
            throw new GerberParseException(position(cursor), shape);
        }
        return cursor.next();
    }

    /** The words of the block at the cursor, without reading them; a comment's text is one of them. */
    static List<String> wordsAhead(TokenCursor cursor) {
        List<String> words = new ArrayList<>();
        for (int offset = 0; ; offset++) {
            Token token = cursor.lookAt(offset);
            if (!GerberTokenKind.WORD.matches(token) && !GerberTokenKind.DATA.matches(token)) {
                return words;
            }
            words.add(token.value());
        }
    }

    /** The words of the block at the cursor, read with its end. */
    static List<String> readWords(TokenCursor cursor) {
        List<String> words = wordsAhead(cursor);
        for (int index = 0; index < words.size(); index++) {
            cursor.next();
        }
        expect(cursor, GerberTokenKind.END_OF_BLOCK, "a block ends with '*'");
        return words;
    }
}
