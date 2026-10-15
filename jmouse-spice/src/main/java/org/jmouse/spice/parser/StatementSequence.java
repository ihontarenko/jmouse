package org.jmouse.spice.parser;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Trivia;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.spice.SpiceParseException;
import org.jmouse.spice.SpicePosition;
import org.jmouse.spice.lexer.SpiceTokenKind;
import org.jmouse.spice.node.SpiceCard;
import org.jmouse.spice.node.SpiceDeck;
import org.jmouse.spice.node.SpiceNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads statements into a deck or a subcircuit until what closes it: each card handed to the parser the
 * {@link ParserContext} finds for it, the comment lines before it hung on it as leading trivia.
 */
final class StatementSequence {

    private StatementSequence() {
    }

    /**
     * @param closing the word that closes the sequence — {@code .ends} for a subcircuit —, or {@code null} for a deck,
     *                which ends at {@code .end} or at the end of the text
     */
    static void read(TokenCursor cursor, SpiceNode parent, ParserContext context, String closing) {
        List<Trivia> pending = new ArrayList<>();
        while (true) {
            Token token = cursor.peek();
            SpicePosition position = SpicePosition.of(token, cursor.source());
            if (SpiceTokenKind.END_OF_TEXT.matches(token)) {
                if (closing != null) {
                    throw new SpiceParseException(parent.position(), "'.subckt' is never closed by '" + closing + "'");
                }
                close(parent, pending);
                return;
            }
            if (SpiceTokenKind.COMMENT.matches(token)) {
                pending.add(Trivia.comment(cursor.next().value()));
                continue;
            }
            if (SpiceTokenKind.END_OF_CARD.matches(token)) {
                cursor.next();
                continue;
            }
            if (!SpiceTokenKind.WORD.matches(token)) {
                throw new SpiceParseException(position, "a card begins with a word: an element's name or a dot card");
            }
            if (closing != null && SpiceTokenKind.isWord(token, closing)) {
                CardReader.read(cursor);
                close(parent, pending);
                return;
            }
            if (SpiceTokenKind.isWord(token, ".ends") || SpiceTokenKind.isWord(token, ".endc")) {
                throw CardParsers.stray(token, position);
            }
            Parser parser = context.getParser(cursor);
            if (parser == null) {
                throw new SpiceParseException(position, "'" + token.value() + "' begins no card SPICE has");
            }
            parser.parse(cursor, parent, context);
            parent.getLast().addLeadingTrivia(pending);
            pending.clear();
            if (parent.getLast() instanceof SpiceCard.End) {
                if (closing != null) {
                    throw new SpiceParseException(position, "'.end' inside a subcircuit; '" + closing + "' closes it first");
                }
                ((SpiceDeck) parent).ignoredAfterEnd(cardsLeft(cursor));
                return;
            }
        }
    }

    private static void close(SpiceNode parent, List<Trivia> pending) {
        if (pending.isEmpty()) {
            return;
        }
        switch (parent) {
            case SpiceDeck deck -> deck.addClosingTrivia(pending);
            case SpiceCard.Subcircuit subcircuit -> subcircuit.addClosingTrivia(pending);
            default -> parent.addLeadingTrivia(pending);
        }
    }

    /** How many cards are left after {@code .end}; the cursor is moved past them. */
    private static int cardsLeft(TokenCursor cursor) {
        int cards = 0;
        while (!SpiceTokenKind.END_OF_TEXT.matches(cursor.peek())) {
            if (SpiceTokenKind.END_OF_CARD.matches(cursor.next())) {
                cards++;
            }
        }
        return cards;
    }
}
