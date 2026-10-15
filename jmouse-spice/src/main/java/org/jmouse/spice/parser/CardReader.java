package org.jmouse.spice.parser;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.Trivia;
import org.jmouse.spice.SpiceNumbers;
import org.jmouse.spice.SpiceParseException;
import org.jmouse.spice.SpicePosition;
import org.jmouse.spice.lexer.SpiceTokenKind;
import org.jmouse.spice.node.SpiceArgument;
import org.jmouse.spice.node.SpiceExpression;
import org.jmouse.spice.node.SpiceParameter;
import org.jmouse.spice.node.SpiceValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads one card off the cursor — from its first word to its {@link SpiceTokenKind#END_OF_CARD} — into its arguments,
 * grouped the way every card groups them: {@code name=value} into an {@link SpiceArgument.Assignment},
 * {@code word(…)} into a {@link SpiceArgument.Group}, commas dropped. What a card's arguments mean is each statement
 * parser's; this is the grammar they share.
 */
final class CardReader {

    private CardReader() {
    }

    /**
     * One card as read.
     *
     * @param head      its first word: an element's name or a dot card's word
     * @param arguments what follows the head, grouped
     * @param trailing  the comment after it on its line, or {@code null}
     * @param inner     comment lines that stood between its lines; they lead the next statement
     * @param tokens    every token of the card after the head, in order, for a parser that needs the text as written
     */
    record Card(Token head, SpicePosition position, List<SpiceArgument> arguments, Trivia trailing, List<Trivia> inner,
                List<Token> tokens, TokenizableSource source) {

        /** The text of the card from token {@code from} of {@link #tokens()} to its end, continuation marks taken out. */
        String textFrom(int from) {
            if (from >= tokens.size()) {
                return "";
            }
            Token first = tokens.get(from);
            Token last = tokens.getLast();
            String written = source.subSequence(first.offset(), last.offset() + last.value().length()).toString();
            return written.replaceAll("[\\r\\n]+\\s*\\+", " ").strip();
        }

        /** The words of the card after the head, refused where an argument is not a word. */
        List<String> words(int from, int to, String shape) {
            List<String> words = new ArrayList<>();
            for (SpiceArgument argument : arguments.subList(from, to)) {
                if (!(argument instanceof SpiceArgument.Word word)) {
                    throw new SpiceParseException(argument.position(), head.value() + ": " + shape);
                }
                words.add(word.text());
            }
            return words;
        }

        /** Refuses a card that has fewer arguments than its shape needs. */
        void requireAtLeast(int count, String shape) {
            if (arguments.size() < count) {
                throw new SpiceParseException(position, head.value() + ": " + shape);
            }
        }
    }

    static Card read(TokenCursor cursor) {
        TokenizableSource source = cursor.source();
        Token head = cursor.next();
        SpicePosition position = SpicePosition.of(head, source);
        List<Token> tokens = new ArrayList<>();
        List<Trivia> inner = new ArrayList<>();
        Trivia trailing = null;
        while (!SpiceTokenKind.END_OF_CARD.matches(cursor.peek()) && !SpiceTokenKind.END_OF_TEXT.matches(cursor.peek())) {
            Token token = cursor.next();
            if (SpiceTokenKind.TRAILING_COMMENT.matches(token)) {
                trailing = Trivia.comment(token.value());
            } else if (SpiceTokenKind.COMMENT.matches(token)) {
                inner.add(Trivia.comment(token.value()));
            } else if (SpiceTokenKind.UNKNOWN.matches(token)) {
                throw new SpiceParseException(SpicePosition.of(token, source), "'" + token.value() + "' has no place here");
            } else {
                tokens.add(token);
            }
        }
        cursor.consumeIf(SpiceTokenKind.END_OF_CARD);
        Arguments arguments = new Arguments(tokens, source);
        return new Card(head, position, arguments.all(), trailing, inner, tokens, source);
    }

    // ─── values ─────────────────────────────────────────────────────────────────────────────────────

    /** A value: a number with its scale factor, a parameter's name, or an expression. */
    static SpiceValue value(SpiceArgument argument, String what) {
        return switch (argument) {
            case SpiceArgument.Word word -> {
                if (SpiceNumbers.read(word.text()).isPresent()) {
                    yield new SpiceValue.Number(SpiceNumbers.read(word.text()).get().value(), word.text(), word.position());
                }
                char first = word.text().charAt(0);
                if (Character.isDigit(first) || first == '.' || first == '-' || first == '+') {
                    throw new SpiceParseException(word.position(), "'" + word.text() + "' is not a number SPICE reads: "
                            + what + " is a number such as 4.7k or 10uF — 1k5 is written 1.5k");
                }
                yield new SpiceValue.Reference(word.text(), word.position());
            }
            case SpiceArgument.Expression expression -> new SpiceValue.Expression(expression.expression());
            default -> throw new SpiceParseException(argument.position(), what + " is a number, a parameter or an "
                    + "expression in braces");
        };
    }

    /** {@code name=value} arguments as parameters; anything else refused with the shape expected. */
    static List<SpiceParameter> parameters(List<SpiceArgument> arguments, String shape) {
        List<SpiceParameter> parameters = new ArrayList<>();
        for (SpiceArgument argument : arguments) {
            if (!(argument instanceof SpiceArgument.Assignment assignment)) {
                throw new SpiceParseException(argument.position(), shape);
            }
            parameters.add(new SpiceParameter(assignment.name(), value(assignment.value(), assignment.name()),
                    assignment.position()));
        }
        return parameters;
    }

    /** A word where a word is needed: a node, a model's name. */
    static String word(SpiceArgument argument, String what) {
        if (!(argument instanceof SpiceArgument.Word word)) {
            throw new SpiceParseException(argument.position(), what + " is a word");
        }
        return word.text();
    }

    // ─── grouping ───────────────────────────────────────────────────────────────────────────────────

    /** The tokens of one card after its head, grouped into arguments. */
    private static final class Arguments {

        private final List<Token>       tokens;
        private final TokenizableSource source;
        private int index;

        Arguments(List<Token> tokens, TokenizableSource source) {
            this.tokens = tokens;
            this.source = source;
        }

        List<SpiceArgument> all() {
            List<SpiceArgument> arguments = new ArrayList<>();
            while (index < tokens.size()) {
                Token token = tokens.get(index);
                if (SpiceTokenKind.CLOSE.matches(token)) {
                    throw new SpiceParseException(position(token), "this ')' closes no '('");
                }
                if (SpiceTokenKind.COMMA.matches(token)) {
                    index++;
                    continue;
                }
                arguments.add(argument());
            }
            return arguments;
        }

        private SpiceArgument argument() {
            SpiceArgument value = value();
            if (index < tokens.size() && SpiceTokenKind.EQUALS.matches(tokens.get(index))) {
                Token equals = tokens.get(index++);
                if (!(value instanceof SpiceArgument.Word name)) {
                    throw new SpiceParseException(position(equals), "'=' follows a name: 'IS=1n', 'R={2*R1}'");
                }
                if (index >= tokens.size() || SpiceTokenKind.CLOSE.matches(tokens.get(index))
                        || SpiceTokenKind.EQUALS.matches(tokens.get(index)) || SpiceTokenKind.COMMA.matches(tokens.get(index))) {
                    throw new SpiceParseException(position(equals), "'" + name.text() + "=' has no value after it");
                }
                return new SpiceArgument.Assignment(name.text(), value(), name.position());
            }
            return value;
        }

        private SpiceArgument value() {
            Token token = tokens.get(index++);
            SpicePosition position = position(token);
            SpiceTokenKind kind = (SpiceTokenKind) token.type();
            return switch (kind) {
                case WORD -> {
                    if (index < tokens.size() && SpiceTokenKind.OPEN.matches(tokens.get(index))) {
                        index++;
                        yield new SpiceArgument.Group(token.value(), group(token), position);
                    }
                    yield new SpiceArgument.Word(token.value(), position);
                }
                case EXPRESSION -> new SpiceArgument.Expression(SpiceExpression.of(token.value(), position));
                case TEXT -> new SpiceArgument.Text(token.value().substring(1, token.value().length() - 1), position);
                case OPEN -> new SpiceArgument.Group("", group(token), position);
                case EQUALS -> throw new SpiceParseException(position, "'=' follows a name: 'IS=1n'");
                case CLOSE -> throw new SpiceParseException(position, "this ')' closes no '('");
                default -> throw new SpiceParseException(position, "'" + token.value() + "' has no place here");
            };
        }

        private List<SpiceArgument> group(Token opening) {
            List<SpiceArgument> arguments = new ArrayList<>();
            while (true) {
                if (index >= tokens.size()) {
                    throw new SpiceParseException(position(opening), "the '(' after '" + opening.value() + "' never closes");
                }
                Token token = tokens.get(index);
                if (SpiceTokenKind.CLOSE.matches(token)) {
                    index++;
                    return arguments;
                }
                if (SpiceTokenKind.COMMA.matches(token)) {
                    index++;
                    continue;
                }
                arguments.add(argument());
            }
        }

        private SpicePosition position(Token token) {
            return SpicePosition.of(token, source);
        }
    }
}
