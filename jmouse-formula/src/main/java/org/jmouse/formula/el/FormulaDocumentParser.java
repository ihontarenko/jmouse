package org.jmouse.formula.el;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.language.parser.StatementsParser;
import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Cursor;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.ExpressionParser;
import org.jmouse.el.parser.ParseException;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.formula.FormulaException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A formula document: definitions, then the formula, one statement after another.
 *
 * <pre>
 * # a comment runs to the end of its line
 * r = sqrt(x^2 + y^2)
 * f(u, v) = exp(-u^2) cos(v)
 * f(r, 3 r)
 * </pre>
 *
 * <p>A statement ends at a {@code ;} or at the end of its line — unless the line is still inside parentheses or
 * brackets, or ends in an operator or a comma, as {@code r = x +} does; then the statement goes on to the next line.
 * One {@code =} at the top level makes the statement {@code left = right}; {@code ==} is a comparison everywhere.</p>
 *
 * <p>Where a statement ends is this grammar's to say; what it holds is read by jMF's expression parser, which never
 * sees a line break or a comment.</p>
 */
final class FormulaDocumentParser extends StatementsParser {

    /** What a line ends in when the statement goes on to the next: an operator, a comma, an {@code =}. */
    private static final String CONTINUING = "+-*/^,=<>&|!%";

    /** The word after a range that makes its variable a whole index: {@code k in 1 .. N integer}. */
    private static final String INTEGER = "integer";

    @Override
    protected void open(TokenCursor cursor) {
        cursor.consumeIf(BasicToken.T_SOL);
    }

    @Override
    protected boolean isClosed(TokenCursor cursor) {
        return !cursor.hasNext() || cursor.isCurrent(BasicToken.T_EOL);
    }

    @Override
    protected void close(TokenCursor cursor) {
        cursor.consumeIf(BasicToken.T_EOL);
    }

    @Override
    protected Expression statement(TokenCursor cursor, ParserContext context) {
        List<Token> written = new ArrayList<>();
        List<Token> meaningful = new ArrayList<>();
        Token end = extent(cursor, written, meaningful);
        cursor.shift(written.size());
        int offset = meaningful.getFirst().offset();
        int ending = startOf(end, cursor.source());
        List<FormulaStatement.Range> ranges = new ArrayList<>();
        List<Token> body = withoutRanges(meaningful, written, end, cursor, context, ranges);
        Token bodyEnd = ranges.isEmpty() ? end : written.get(written.indexOf(body.getLast()) + 1);
        List<Integer> equals = topLevelEquals(body);
        if (equals.size() > 1) {
            throw new FormulaException("An equation has one '='; compare with '==' instead.",
                    sourceText(cursor), body.get(equals.get(1)).offset());
        }
        if (equals.isEmpty()) {
            return new FormulaStatementNode(new FormulaStatement.Plain(side(body, written, bodyEnd, cursor, context),
                    ranges, offset, ending));
        }
        int at = equals.getFirst();
        List<Token> left = body.subList(0, at);
        List<Token> right = body.subList(at + 1, body.size());
        if (left.isEmpty() || right.isEmpty()) {
            throw new FormulaException("A side of the formula is empty.", sourceText(cursor), offset);
        }
        Token equalSign = body.get(at);
        return new FormulaStatementNode(new FormulaStatement.Assignment(head(left),
                side(left, written, equalSign, cursor, context), side(right, written, bodyEnd, cursor, context), ranges,
                offset, ending));
    }

    /**
     * The statement without the ranges written after it, which are gathered into {@code ranges} in order:
     * {@code (cos s, sin s)  s in 0 .. 2 pi  k in 1 .. N integer} is its tuple, then {@code s}'s range and {@code k}'s.
     * A range is a name, {@code in}, two bounds around {@code ..} — all outside parentheses —, and {@code integer} for a
     * whole index; it stands after the statement's own tokens, which never end up empty.
     */
    private static List<Token> withoutRanges(List<Token> meaningful, List<Token> written, Token end, TokenCursor cursor,
                                             ParserContext context, List<FormulaStatement.Range> ranges) {
        int length = meaningful.size();
        Token after = end;
        while (true) {
            boolean integer = length > 0 && meaningful.get(length - 1).type() == BasicToken.T_IDENTIFIER
                    && meaningful.get(length - 1).value().equals(INTEGER);
            int boundsEnd = integer ? length - 1 : length;
            int in = -1;
            int dots = -1;
            int depth = 0;
            for (int index = 0; index < boundsEnd; index++) {
                Token.Type type = meaningful.get(index).type();
                if (type == BasicToken.T_OPEN_PAREN || type == BasicToken.T_OPEN_BRACKET) {
                    depth++;
                } else if (type == BasicToken.T_CLOSE_PAREN || type == BasicToken.T_CLOSE_BRACKET) {
                    depth--;
                } else if (depth == 0 && type == BasicToken.T_IN && index > 1
                        && meaningful.get(index - 1).type() == BasicToken.T_IDENTIFIER) {
                    in = index;
                    dots = -1;
                } else if (depth == 0 && type == BasicToken.T_DOUBLE_DOT && in >= 0 && dots < 0) {
                    dots = index;
                }
            }
            if (in < 0 || dots < 0 || dots == in + 1 || dots == boundsEnd - 1) {
                return meaningful.subList(0, length);
            }
            Token dotsToken = meaningful.get(dots);
            Token toEnd = integer ? meaningful.get(boundsEnd) : after;
            FormulaStatement.Side from = side(meaningful.subList(in + 1, dots), written, dotsToken, cursor, context);
            FormulaStatement.Side to = side(meaningful.subList(dots + 1, boundsEnd), written, toEnd, cursor, context);
            Token variable = meaningful.get(in - 1);
            ranges.addFirst(new FormulaStatement.Range(variable.value(), from, to, integer, variable.offset()));
            length = in - 1;
            after = variable;
        }
    }

    /**
     * Gathers the statement the cursor stands on, without moving it.
     *
     * @param written    every token of the statement, comments and joined line breaks included
     * @param meaningful the tokens the expression parser reads
     * @return the token that ends the statement: a {@code ;}, a line break, or the end of the text
     */
    private static Token extent(TokenCursor cursor, List<Token> written, List<Token> meaningful) {
        int depth = 0;
        boolean comment = false;
        for (int ahead = 0; ; ahead++) {
            Token token = cursor.lookAt(ahead);
            Token.Type type = token.type();
            if (type == BasicToken.T_EOL) {
                return token;
            }
            if (type == BasicToken.T_NEW_LINE) {
                comment = false;
                if (depth <= 0 && !continues(meaningful)) {
                    return token;
                }
            } else if (type == BasicToken.T_HASH) {
                comment = true;
            } else if (!comment) {
                if (depth <= 0 && type == BasicToken.T_SEMICOLON) {
                    return token;
                }
                if (type == BasicToken.T_OPEN_PAREN || type == BasicToken.T_OPEN_BRACKET) {
                    depth++;
                } else if (type == BasicToken.T_CLOSE_PAREN || type == BasicToken.T_CLOSE_BRACKET) {
                    depth--;
                }
                meaningful.add(token);
            }
            written.add(token);
        }
    }

    /** Whether the statement so far ends in what carries it over a line break. */
    private static boolean continues(List<Token> meaningful) {
        if (meaningful.isEmpty()) {
            return true;
        }
        Token last = meaningful.getLast();
        String value = last.value();
        return last.type() != BasicToken.T_STRING && !value.isEmpty()
                && CONTINUING.indexOf(value.charAt(value.length() - 1)) >= 0;
    }

    /** Where a single {@code =} stands outside parentheses and brackets. */
    private static List<Integer> topLevelEquals(List<Token> meaningful) {
        List<Integer> found = new ArrayList<>();
        int depth = 0;
        for (int index = 0; index < meaningful.size(); index++) {
            Token token = meaningful.get(index);
            Token.Type type = token.type();
            if (type == BasicToken.T_OPEN_PAREN || type == BasicToken.T_OPEN_BRACKET) {
                depth++;
            } else if (type == BasicToken.T_CLOSE_PAREN || type == BasicToken.T_CLOSE_BRACKET) {
                depth--;
            } else if (depth == 0 && type == BasicToken.T_EQ && token.value().equals("=")) {
                found.add(index);
            }
        }
        return found;
    }

    /** {@code name} or {@code name(parameter, …)}, when the left side is written as nothing else. */
    private static Optional<FormulaStatement.Head> head(List<Token> left) {
        if (left.getFirst().type() != BasicToken.T_IDENTIFIER) {
            return Optional.empty();
        }
        String name = left.getFirst().value();
        if (left.size() == 1) {
            return Optional.of(new FormulaStatement.Head(name, List.of()));
        }
        if (left.get(1).type() != BasicToken.T_OPEN_PAREN || left.getLast().type() != BasicToken.T_CLOSE_PAREN) {
            return Optional.empty();
        }
        List<String> parameters = new ArrayList<>();
        for (int index = 2; index < left.size() - 1; index++) {
            boolean expectsName = (index - 2) % 2 == 0;
            Token token = left.get(index);
            if (expectsName && token.type() == BasicToken.T_IDENTIFIER) {
                parameters.add(token.value());
            } else if (expectsName || token.type() != BasicToken.T_COMMA) {
                return Optional.empty();
            }
        }
        if (left.size() > 3 && left.get(left.size() - 2).type() == BasicToken.T_COMMA) {
            return Optional.empty();
        }
        return Optional.of(new FormulaStatement.Head(name, List.copyOf(parameters)));
    }

    /**
     * One side of a statement, read by jMF's expression parser over its own tokens — line breaks and comments left
     * out —, and as written.
     *
     * @param tokens  the side's tokens
     * @param written every token of the statement, to cut the text between them from the source
     * @param end     the token after the side
     */
    private static FormulaStatement.Side side(List<Token> tokens, List<Token> written, Token end, TokenCursor cursor,
                                              ParserContext context) {
        List<Token> numbered = new ArrayList<>(tokens.size() + 1);
        for (Token token : tokens) {
            numbered.add(new Token(token.value(), token.type(), numbered.size(), token.offset(), token.lineNumber()));
        }
        Token last = tokens.getLast();
        int endOffset = startOf(following(last, written, end), cursor.source());
        numbered.add(new Token("", BasicToken.T_EOL, numbered.size(), endOffset, last.lineNumber()));
        TokenCursor reading = new Cursor(cursor.source(), numbered);
        String text = text(tokens, written, end, cursor.source());
        Expression expression = (Expression) context.getParser(ExpressionParser.class).parse(reading, context);
        if (!reading.isCurrent(BasicToken.T_EOL)) {
            Token unread = reading.peek();
            throw new ParseException("'%s' was only partly read: the parse stopped at '%s'".formatted(text,
                    unread.value()));
        }
        Set<String> names = new LinkedHashSet<>();
        for (Token token : tokens) {
            if (token.type() == BasicToken.T_IDENTIFIER) {
                names.add(token.value());
            }
        }
        return new FormulaStatement.Side(expression, text, Set.copyOf(names), tokens);
    }

    /**
     * The side as written: each token with what follows it up to the next, and a single space where a line break or a
     * comment stood between two of them.
     */
    private static String text(List<Token> tokens, List<Token> written, Token end, TokenizableSource source) {
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < tokens.size(); index++) {
            Token token = tokens.get(index);
            Token following = following(token, written, end);
            String piece = source.subSequence(token.offset(), startOf(following, source)).toString();
            boolean joined = index + 1 < tokens.size() && !following.equals(tokens.get(index + 1));
            text.append(joined ? piece.stripTrailing() + " " : piece);
        }
        return text.toString().strip();
    }

    /** Where a token starts in the source; the end of the text for the end marker, which stands past it. */
    private static int startOf(Token token, TokenizableSource source) {
        return Math.min(token.offset(), source.length());
    }

    /** The token written right after this one in the statement, or the token that ends it. */
    private static Token following(Token token, List<Token> written, Token end) {
        int index = written.indexOf(token);
        return index + 1 < written.size() ? written.get(index + 1) : end;
    }

    private static String sourceText(TokenCursor cursor) {
        TokenizableSource source = cursor.source();
        return source.subSequence(0, source.length()).toString();
    }

    /** A statement in the parse tree. */
    static final class FormulaStatementNode extends AbstractExpression {

        private final FormulaStatement statement;

        FormulaStatementNode(FormulaStatement statement) {
            this.statement = statement;
        }

        FormulaStatement statement() {
            return statement;
        }

        @Override
        public Object evaluate(EvaluationContext context) {
            return this;
        }

        @Override
        public String toString() {
            return statement.toString();
        }
    }
}
