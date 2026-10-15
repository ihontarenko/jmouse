package org.jmouse.spice.parser;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.spice.lexer.SpiceTokenKind;
import org.jmouse.spice.node.SpiceStatement;

/**
 * A jME {@link Parser} for one kind of SPICE card: it says which cards are its by their first word, and builds the
 * statement node of one. Every card parser is registered in the {@link ParserContext}, which picks the one that
 * {@link #supports supports} the card at the cursor — so a new kind of card is a new parser, not a new branch.
 *
 * <p>A card's comments travel with its node: the comment after it on its line is its trailing trivia, comment lines
 * between its continuation lines lead it.</p>
 */
abstract class CardParser implements Parser {

    /** Whether a card that begins with this word is this parser's. */
    abstract boolean accepts(Token head);

    /** The statement the card is; {@code cursor} stands after the card, for a parser whose statement spans cards. */
    abstract SpiceStatement statement(CardReader.Card card, TokenCursor cursor, ParserContext context);

    @Override
    public boolean supports(TokenCursor cursor) {
        Token head = cursor.peek();
        return SpiceTokenKind.WORD.matches(head) && accepts(head);
    }

    @Override
    public void parse(TokenCursor cursor, Node parent, ParserContext context) {
        CardReader.Card card = CardReader.read(cursor);
        SpiceStatement statement = statement(card, cursor, context);
        statement.addLeadingTrivia(card.inner());
        if (card.trailing() != null) {
            statement.setTrailingTrivia(card.trailing());
        }
        parent.add(statement);
    }

    /** Whether a head is an element whose letter is one of these. */
    static boolean isElement(Token head, String letters) {
        String value = head.value();
        return !value.startsWith(".") && letters.indexOf(Character.toUpperCase(value.charAt(0))) >= 0;
    }

    /** Whether a head is one of these dot cards. */
    static boolean isDirective(Token head, String... directives) {
        String value = SpiceTokenKind.lower(head);
        for (String directive : directives) {
            if (value.equals(directive)) {
                return true;
            }
        }
        return false;
    }
}
