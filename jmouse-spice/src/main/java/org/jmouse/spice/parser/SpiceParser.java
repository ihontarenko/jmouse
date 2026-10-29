package org.jmouse.spice.parser;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.spice.SpicePosition;
import org.jmouse.spice.lexer.SpiceLexer;
import org.jmouse.spice.lexer.SpiceTokenKind;
import org.jmouse.spice.node.SpiceDeck;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads a SPICE text into its {@link SpiceDeck}: the {@link SpiceLexer} cuts it into tokens, and a jME
 * {@link ParserContext} holding one parser per kind of card reads them, each card's parser chosen by the card's first
 * word.
 *
 * <p>Reading is structural: a resistor without a value, a parenthesis that never closes, a {@code .subckt} without
 * {@code .ends} are refused with their line and column ({@link org.jmouse.spice.SpiceParseException}). A card the
 * reader has no dedicated node for — a switch, an {@code .options} line — is kept as a generic one, so nothing that was
 * written is lost on its way back through the nodes' own {@code toSource()}.</p>
 */
public final class SpiceParser {

    private final SpiceLexer lexer = new SpiceLexer();

    /** Reads a text that has no name of its own. */
    public SpiceDeck parse(String text) {
        return parse("<netlist>", text);
    }

    /** @param name what the text is called in a refusal: a file name */
    public SpiceDeck parse(String name, String text) {
        TokenCursor cursor = lexer.tokenize(new StringSource(name, text));
        ParserContext context = context(modelNames(cursor));
        Token first = cursor.peek();
        SpiceDeck deck;
        if (SpiceTokenKind.TITLE.matches(first)) {
            cursor.next();
            deck = new SpiceDeck(first.value(), SpicePosition.of(first, cursor.source()));
        } else {
            deck = new SpiceDeck(null, new SpicePosition(1, 1));
        }
        StatementSequence.read(cursor, deck, context, null);
        return deck;
    }

    /** One parser per kind of card. Their cards never overlap, so the order the context asks them in does not matter. */
    private static ParserContext context(Set<String> models) {
        ParserContext context = new DefaultParserContext();
        List.of(new ElementParsers.PassiveParser(), new ElementParsers.SourceParser(), new ElementParsers.DiodeParser(),
                new ElementParsers.BipolarParser(models), new ElementParsers.MosfetParser(),
                new ElementParsers.InstanceParser(), new ElementParsers.BehaviouralParser(),
                new ElementParsers.ControlledParser(), new ElementParsers.CouplingParser(), new ElementParsers.GenericParser(),
                new CardParsers.ModelParser(), new CardParsers.SubcircuitParser(), new CardParsers.ParametersParser(),
                new CardParsers.IncludeParser(), new CardParsers.TransientParser(), new CardParsers.AcParser(),
                new CardParsers.DcParser(), new CardParsers.OperatingPointParser(), new CardParsers.ControlParser(),
                new CardParsers.EndParser(), new CardParsers.DirectiveParser()).forEach(context::addParser);
        return context;
    }

    /**
     * The names every {@code .model} card declares, wherever it stands: a {@code Q} card's fourth word is a substrate or
     * its model depending on them, and a model may be declared after the elements that use it.
     */
    private static Set<String> modelNames(TokenCursor cursor) {
        Set<String> names = new HashSet<>();
        Token previous = null;
        for (Token token : cursor) {
            if (previous != null && SpiceTokenKind.isWord(previous, ".model") && SpiceTokenKind.WORD.matches(token)) {
                names.add(SpiceTokenKind.lower(token));
            }
            previous = token;
        }
        return names;
    }
}
