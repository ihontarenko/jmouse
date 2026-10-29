package org.jmouse.gerber.excellon.parser;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.gerber.GerberPosition;
import org.jmouse.gerber.excellon.lexer.ExcellonLexer;
import org.jmouse.gerber.excellon.lexer.ExcellonTokenKind;
import org.jmouse.gerber.excellon.node.ExcellonProgram;

import java.util.List;

/**
 * Reads an Excellon text into its {@link ExcellonProgram}: the {@link ExcellonLexer} cuts it into lines of tokens, and
 * a jME {@link ParserContext} holding one parser per kind of statement reads them. A line no parser is named for is kept
 * as written, so the program's {@code toSource()} gives the file back.
 */
public final class ExcellonParser {

    private static final Parser OTHER = new StatementParsers.OtherParser();

    private final ExcellonLexer lexer = new ExcellonLexer();

    public ExcellonProgram parse(String text) {
        return parse("<excellon>", text);
    }

    /** @param name what the text is called in a refusal: a file name */
    public ExcellonProgram parse(String name, String text) {
        TokenCursor cursor = lexer.tokenize(new StringSource(name, text));
        ParserContext context = context();
        ExcellonProgram program = new ExcellonProgram(new GerberPosition(1, 1));
        while (!ExcellonTokenKind.END_OF_TEXT.matches(cursor.peek())) {
            Parser parser = context.getParser(cursor);
            (parser == null ? OTHER : parser).parse(cursor, program, context);
        }
        return program;
    }

    /** One parser per kind of statement. Their lines never overlap, so the order the context asks them in does not matter. */
    private static ParserContext context() {
        ParserContext context = new DefaultParserContext();
        List.of(new StatementParsers.CommentParser(), new StatementParsers.HeaderStartParser(),
                new StatementParsers.HeaderEndParser(), new StatementParsers.FormatParser(),
                new StatementParsers.UnitParser(), new StatementParsers.ToolDefinitionParser(),
                new StatementParsers.ToolSelectParser(), new StatementParsers.CodeParser(),
                new StatementParsers.DrillParser()).forEach(context::addParser);
        return context;
    }
}
