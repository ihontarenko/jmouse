package org.jmouse.svg.path.parser;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.BasicNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.svg.path.lexer.PathLexer;
import org.jmouse.svg.path.lexer.PathTokenKind;
import org.jmouse.svg.path.node.PathCommand;
import org.jmouse.svg.path.node.PathCommandKind;
import org.jmouse.svg.path.node.PathData;

import java.util.List;

/**
 * Reads SVG path data into its {@link PathData}: the {@link PathLexer} cuts it into letters and numbers, and a jME
 * {@link ParserContext} holding the {@link PathCommandParser} reads it a command at a time. Stateless and safe to share.
 */
public final class PathParser {

    private final PathLexer lexer = new PathLexer();

    public PathData parse(String text) {
        return parse("<path>", text);
    }

    /** @param name what the text is called in a refusal */
    public PathData parse(String name, String text) {
        TokenCursor cursor = lexer.tokenize(new StringSource(name, text));
        ParserContext context = new DefaultParserContext();
        context.addParser(new PathCommandParser());
        Node commands = BasicNode.forToken(cursor.peek());
        while (!PathTokenKind.END_OF_TEXT.matches(cursor.peek())) {
            Token token = cursor.peek();
            Parser parser = context.getParser(cursor);
            if (parser == null) {
                throw new PathParseException(name, token.offset(), "'" + token.value() + "' is not path data: every command "
                        + "opens with its letter, " + letters());
            }
            parser.parse(cursor, commands, context);
        }
        List<PathCommand> read = commands.getChildren().stream().map(PathCommand.class::cast).toList();
        if (!read.isEmpty() && read.getFirst().kind() != PathCommandKind.MOVE) {
            throw new PathParseException(name, 0, "path data opens with a move: 'M x y'");
        }
        return new PathData(read);
    }

    private static String letters() {
        StringBuilder letters = new StringBuilder();
        for (PathCommandKind kind : PathCommandKind.values()) {
            letters.append(kind.letter());
        }
        return letters.toString();
    }
}
