package org.jmouse.strokefont.parser;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.strokefont.FontoBeneParseException;
import org.jmouse.strokefont.lexer.FontoBeneLexer;
import org.jmouse.strokefont.lexer.FontoBeneTokenKind;
import org.jmouse.strokefont.node.FontoBeneDocument;
import org.jmouse.strokefont.node.FontoBeneLine;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads a FontoBene text into its tree: each line the lexer said what it is becomes that line's node, its numbers read.
 * A line that breaks the format is refused with where it stands.
 */
public final class FontoBeneParser {

    private final FontoBeneLexer lexer = new FontoBeneLexer();

    public FontoBeneDocument parse(String text) {
        return parse("<fontobene>", text);
    }

    public FontoBeneDocument parse(String name, String text) {
        TokenCursor cursor = lexer.tokenize(new StringSource(name, text));
        FontoBeneDocument document = new FontoBeneDocument();
        while (!FontoBeneTokenKind.END_OF_TEXT.matches(cursor.peek())) {
            Token token = cursor.next();
            if (!FontoBeneTokenKind.END_OF_LINE.matches(token)) {
                document.add(lineOf(token));
            }
        }
        return document;
    }

    private static FontoBeneLine lineOf(Token token) {
        String text = token.value();
        int line = token.lineNumber();
        return switch ((FontoBeneTokenKind) token.type()) {
            case COMMENT -> new FontoBeneLine.Comment(text.substring(1), line);
            case SECTION -> new FontoBeneLine.Section(text.substring(1, text.length() - 1), line);
            case ENTRY -> entry(text, line);
            case SEPARATOR -> new FontoBeneLine.Separator(line);
            case GLYPH -> new FontoBeneLine.Glyph(Integer.parseInt(text.substring(1, text.indexOf(']')), 16),
                    text.substring(text.indexOf(']') + 1).strip(), line);
            case REFERENCE -> new FontoBeneLine.Reference(codepoint(text.substring(1), line), line);
            case SPACING -> new FontoBeneLine.Spacing(number(text.substring(1), line), line);
            case POLYLINE -> polyline(text, line);
            case BLANK -> new FontoBeneLine.Blank(line);
            default -> throw new FontoBeneParseException(line, "Not a line of FontoBene: " + text);
        };
    }

    private static FontoBeneLine.Entry entry(String text, int line) {
        int equals = text.indexOf('=');
        if (equals < 0) {
            throw new FontoBeneParseException(line, "A header line is \"key = value\": " + text);
        }
        return new FontoBeneLine.Entry(text.substring(0, equals).strip(), text.substring(equals + 1).strip(), line);
    }

    private static FontoBeneLine.Polyline polyline(String text, int line) {
        List<FontoBeneLine.Vertex> vertices = new ArrayList<>();
        for (String vertex : text.split(";")) {
            String[] parts = vertex.split(",");
            if (parts.length < 2 || parts.length > 3) {
                throw new FontoBeneParseException(line, "A vertex is \"x,y\" or \"x,y,bulge\": " + vertex);
            }
            vertices.add(new FontoBeneLine.Vertex(number(parts[0], line), number(parts[1], line),
                    parts.length == 3 ? number(parts[2], line) : 0));
        }
        return new FontoBeneLine.Polyline(vertices, line);
    }

    private static int codepoint(String text, int line) {
        try {
            return Integer.parseInt(text.strip(), 16);
        } catch (NumberFormatException unreadable) {
            throw new FontoBeneParseException(line, "A codepoint is hexadecimal: " + text);
        }
    }

    private static double number(String text, int line) {
        try {
            return Double.parseDouble(text.strip());
        } catch (NumberFormatException unreadable) {
            throw new FontoBeneParseException(line, "Not a number: " + text);
        }
    }
}
