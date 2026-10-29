package org.jmouse.gerber.parser;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.gerber.GerberPosition;
import org.jmouse.gerber.lexer.GerberLexer;
import org.jmouse.gerber.node.GerberDocument;

import java.util.List;

/**
 * Reads a Gerber text into its {@link GerberDocument}: the {@link GerberLexer} cuts it into tokens, and a jME
 * {@link ParserContext} holding one parser per kind of command reads them.
 *
 * <p>Reading is structural: a block without its {@code *}, an extended command never closed, a region never closed
 * are refused with their line and column ({@link org.jmouse.gerber.GerberParseException}). A command no parser is named
 * for is kept as written, so the document's {@code toSource()} gives the file back.</p>
 */
public final class GerberParser {

    private final GerberLexer lexer = new GerberLexer();

    public GerberDocument parse(String text) {
        return parse("<gerber>", text);
    }

    /** @param name what the text is called in a refusal: a file name */
    public GerberDocument parse(String name, String text) {
        TokenCursor cursor = lexer.tokenize(new StringSource(name, text));
        GerberDocument document = new GerberDocument(new GerberPosition(1, 1));
        CommandSequence.read(cursor, document, context(), null);
        return document;
    }

    /** One parser per kind of command. Their commands never overlap, so the order the context asks them in does not matter. */
    private static ParserContext context() {
        ParserContext context = new DefaultParserContext();
        List.of(new ExtendedCommandParsers.FormatParser(), new ExtendedCommandParsers.UnitParser(),
                new ExtendedCommandParsers.ApertureParser(), new ExtendedCommandParsers.PolarityParser(),
                new ExtendedCommandParsers.AttributeParser(), new WordCommandParsers.CommentParser(),
                new WordCommandParsers.InterpolationParser(), new WordCommandParsers.QuadrantParser(),
                new WordCommandParsers.EndOfFileParser(), new WordCommandParsers.SelectApertureParser(),
                new WordCommandParsers.RegionParser(), new WordCommandParsers.OperationParser()).forEach(context::addParser);
        return context;
    }
}
