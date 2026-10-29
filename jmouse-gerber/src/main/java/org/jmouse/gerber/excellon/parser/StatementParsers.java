package org.jmouse.gerber.excellon.parser;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.gerber.GerberParseException;
import org.jmouse.gerber.GerberPosition;
import org.jmouse.gerber.excellon.lexer.ExcellonTokenKind;
import org.jmouse.gerber.excellon.node.ExcellonStatement;
import org.jmouse.gerber.excellon.node.ExcellonStatement.DrillCoordinates;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The parsers of Excellon statements, one per kind. Each says which lines are its by their tokens and builds the
 * statement of one; the {@link ParserContext} picks the one that supports the line at the cursor — so a new kind of
 * statement is a new parser, not a new branch.
 */
final class StatementParsers {

    private StatementParsers() {
    }

    /** A parser of one kind of line. */
    abstract static class StatementParser implements Parser {

        /** Whether a line of these tokens is this parser's. */
        abstract boolean accepts(List<Token> line);

        abstract ExcellonStatement statement(List<Token> line, GerberPosition position);

        @Override
        public boolean supports(TokenCursor cursor) {
            List<Token> line = lineAhead(cursor);
            return !line.isEmpty() && accepts(line);
        }

        @Override
        public void parse(TokenCursor cursor, Node parent, ParserContext context) {
            GerberPosition position = GerberPosition.of(cursor.peek(), cursor.source());
            List<Token> line = lineAhead(cursor);
            line.forEach(ignored -> cursor.next());
            if (ExcellonTokenKind.END_OF_LINE.matches(cursor.peek())) {
                cursor.next();
            }
            parent.add(statement(line, position));
        }
    }

    /** The tokens of the line at the cursor, without reading them. */
    static List<Token> lineAhead(TokenCursor cursor) {
        List<Token> line = new ArrayList<>();
        for (int offset = 0; ; offset++) {
            Token token = cursor.lookAt(offset);
            if (ExcellonTokenKind.END_OF_LINE.matches(token) || ExcellonTokenKind.END_OF_TEXT.matches(token)) {
                return line;
            }
            line.add(token);
        }
    }

    private static boolean isWord(List<Token> line, int index, String word) {
        return index < line.size() && ExcellonTokenKind.WORD.matches(line.get(index)) && line.get(index).value().equals(word);
    }

    private static GerberParseException refusal(GerberPosition position, String detail) {
        return new GerberParseException(position, detail);
    }

    /** {@code ; text}. */
    static final class CommentParser extends StatementParser {

        @Override
        boolean accepts(List<Token> line) {
            return line.size() == 1 && ExcellonTokenKind.COMMENT.matches(line.getFirst());
        }

        @Override
        ExcellonStatement statement(List<Token> line, GerberPosition position) {
            return new ExcellonStatement.Comment(line.getFirst().value(), position);
        }
    }

    /** {@code M48}. */
    static final class HeaderStartParser extends StatementParser {

        @Override
        boolean accepts(List<Token> line) {
            return line.size() == 1 && isWord(line, 0, "M48");
        }

        @Override
        ExcellonStatement statement(List<Token> line, GerberPosition position) {
            return new ExcellonStatement.HeaderStart(position);
        }
    }

    /** {@code %}. */
    static final class HeaderEndParser extends StatementParser {

        @Override
        boolean accepts(List<Token> line) {
            return line.size() == 1 && ExcellonTokenKind.PERCENT.matches(line.getFirst());
        }

        @Override
        ExcellonStatement statement(List<Token> line, GerberPosition position) {
            return new ExcellonStatement.HeaderEnd(position);
        }
    }

    /** {@code FMAT,2}. */
    static final class FormatParser extends StatementParser {

        @Override
        boolean accepts(List<Token> line) {
            return line.size() == 3 && isWord(line, 0, "FMAT") && ExcellonTokenKind.COMMA.matches(line.get(1))
                    && line.get(2).value().matches("\\d+");
        }

        @Override
        ExcellonStatement statement(List<Token> line, GerberPosition position) {
            return new ExcellonStatement.Format(Integer.parseInt(line.get(2).value()), position);
        }
    }

    /** {@code METRIC}, {@code INCH}, either with {@code ,TZ} or {@code ,LZ}. */
    static final class UnitParser extends StatementParser {

        private static final Set<String> ZEROS = Set.of("TZ", "LZ");

        @Override
        boolean accepts(List<Token> line) {
            boolean unit = isWord(line, 0, "METRIC") || isWord(line, 0, "INCH");
            return unit && (line.size() == 1 || line.size() == 3 && ExcellonTokenKind.COMMA.matches(line.get(1))
                    && ZEROS.contains(line.get(2).value()));
        }

        @Override
        ExcellonStatement statement(List<Token> line, GerberPosition position) {
            return new ExcellonStatement.Unit(isWord(line, 0, "METRIC"),
                    line.size() == 3 ? Optional.of(line.get(2).value()) : Optional.empty(), position);
        }
    }

    /** {@code T1C0.8}. */
    static final class ToolDefinitionParser extends StatementParser {

        private static final Pattern TOOL     = Pattern.compile("T(\\d+)");
        private static final Pattern DIAMETER = Pattern.compile("C(\\d*\\.?\\d+)");

        @Override
        boolean accepts(List<Token> line) {
            return line.size() == 2 && TOOL.matcher(line.get(0).value()).matches()
                    && DIAMETER.matcher(line.get(1).value()).matches();
        }

        @Override
        ExcellonStatement statement(List<Token> line, GerberPosition position) {
            Matcher tool = TOOL.matcher(line.get(0).value());
            Matcher diameter = DIAMETER.matcher(line.get(1).value());
            tool.matches();
            diameter.matches();
            return new ExcellonStatement.ToolDefinition(Integer.parseInt(tool.group(1)), new BigDecimal(diameter.group(1)),
                    position);
        }
    }

    /** {@code T1}, {@code T0}. */
    static final class ToolSelectParser extends StatementParser {

        @Override
        boolean accepts(List<Token> line) {
            return line.size() == 1 && line.getFirst().value().matches("T\\d+");
        }

        @Override
        ExcellonStatement statement(List<Token> line, GerberPosition position) {
            return new ExcellonStatement.ToolSelect(Integer.parseInt(line.getFirst().value().substring(1)), position);
        }
    }

    /** {@code G90}, {@code G05}, {@code M30}. */
    static final class CodeParser extends StatementParser {

        private static final Set<String> CODES = Set.of(ExcellonStatement.Code.ABSOLUTE, ExcellonStatement.Code.DRILL,
                ExcellonStatement.Code.END);

        @Override
        boolean accepts(List<Token> line) {
            return line.size() == 1 && CODES.contains(line.getFirst().value());
        }

        @Override
        ExcellonStatement statement(List<Token> line, GerberPosition position) {
            return new ExcellonStatement.Code(line.getFirst().value(), position);
        }
    }

    /** {@code X1.5Y2} and {@code X1.5Y2G85X3Y2}: coordinates, and a slot's second point after {@code G85}. */
    static final class DrillParser extends StatementParser {

        private static final Pattern COORDINATE = Pattern.compile("([XY])([-+]?\\d*\\.?\\d+)");

        @Override
        boolean accepts(List<Token> line) {
            int slot = slotAt(line);
            List<Token> first = slot < 0 ? line : line.subList(0, slot);
            return coordinates(first).isPresent() && (slot < 0 || coordinates(line.subList(slot + 1, line.size())).isPresent());
        }

        @Override
        ExcellonStatement statement(List<Token> line, GerberPosition position) {
            int slot = slotAt(line);
            if (slot < 0) {
                return new ExcellonStatement.Hole(coordinates(line).orElseThrow(), position);
            }
            DrillCoordinates from = coordinates(line.subList(0, slot)).orElseThrow();
            DrillCoordinates to = coordinates(line.subList(slot + 1, line.size())).orElseThrow();
            if (from.x().isEmpty() || from.y().isEmpty()) {
                throw refusal(position, "a slot starts at a point written whole: X and Y");
            }
            return new ExcellonStatement.Slot(from, to, position);
        }

        private static int slotAt(List<Token> line) {
            for (int index = 0; index < line.size(); index++) {
                if (isWord(line, index, "G85")) {
                    return index;
                }
            }
            return -1;
        }

        /** X then Y, either left out but not both; empty for anything else. */
        private static Optional<DrillCoordinates> coordinates(List<Token> words) {
            BigDecimal x = null;
            BigDecimal y = null;
            for (Token word : words) {
                Matcher matcher = COORDINATE.matcher(word.value());
                if (!ExcellonTokenKind.WORD.matches(word) || !matcher.matches()) {
                    return Optional.empty();
                }
                if (matcher.group(1).equals("X")) {
                    if (x != null || y != null) {
                        return Optional.empty();
                    }
                    x = new BigDecimal(matcher.group(2));
                } else {
                    if (y != null) {
                        return Optional.empty();
                    }
                    y = new BigDecimal(matcher.group(2));
                }
            }
            if (x == null && y == null) {
                return Optional.empty();
            }
            return Optional.of(new DrillCoordinates(Optional.ofNullable(x), Optional.ofNullable(y)));
        }
    }

    /** Any other line, kept as its words. Not one of the context's parsers: what none of them supports falls to it. */
    static final class OtherParser extends StatementParser {

        @Override
        boolean accepts(List<Token> line) {
            return true;
        }

        @Override
        ExcellonStatement statement(List<Token> line, GerberPosition position) {
            return new ExcellonStatement.Other(line.stream().map(Token::value).toList(), position);
        }
    }
}
