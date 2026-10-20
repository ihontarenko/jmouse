package org.jmouse.el.language.line;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * One line of a line language's document, read by a {@link LineReader}: its tokens, and the words a person sees in
 * them. Two tokens with nothing between them are one word — {@code 74hc595}, {@code supply-voltage},
 * {@code data.jmd#supply}, {@code °C} —, unless one of them is a separator.
 */
public final class TokenLine {

    /** What ends a joined word even with no blank before it: {@code a, b} is two words, not {@code "a,"}. */
    private static final Set<String> SEPARATORS = Set.of(",", ";", "=", "(", ")", "[", "]", "{", "}", ":");

    private final LineReader reader;
    private final List<Token> tokens;
    private final int end;

    TokenLine(LineReader reader, List<Token> tokens, int end) {
        this.reader = reader;
        this.tokens = List.copyOf(tokens);
        this.end = end;
    }

    public boolean isEmpty() {
        return tokens.isEmpty();
    }

    public int size() {
        return tokens.size();
    }

    public Token get(int index) {
        return tokens.get(index);
    }

    public String value(int index) {
        return index < tokens.size() ? tokens.get(index).value() : "";
    }

    public boolean isString(int index) {
        return index < tokens.size() && tokens.get(index).type() == BasicToken.T_STRING;
    }

    /** A text's words, its quotes taken off and its escapes read: {@code \"} a quote, {@code \\} a backslash. */
    public String literal(int index) {
        String value = value(index);
        if (!isString(index) || value.length() < 2) {
            return value;
        }
        String inside = value.substring(1, value.length() - 1);
        StringBuilder words = new StringBuilder(inside.length());
        for (int position = 0; position < inside.length(); position++) {
            char character = inside.charAt(position);
            if (character == '\\' && position + 1 < inside.length()) {
                position++;
                character = inside.charAt(position);
            }
            words.append(character);
        }
        return words.toString();
    }

    /** Whether token {@code index + 1} follows token {@code index} with nothing between them, making one word. */
    public boolean joined(int index) {
        if (index + 1 >= tokens.size()) {
            return false;
        }
        Token token = tokens.get(index);
        Token next = tokens.get(index + 1);
        String between = reader.text().substring(token.offset() + token.value().length(), next.offset());
        return between.isEmpty() && !SEPARATORS.contains(token.value()) && !SEPARATORS.contains(next.value())
                && token.type() != BasicToken.T_STRING && next.type() != BasicToken.T_STRING;
    }

    /** The run of joined tokens from {@code from}, as one word. */
    public String joinedWord(int from) {
        int last = joinedEnd(from);
        Token first = tokens.get(from);
        Token closing = tokens.get(last - 1);
        return reader.text().substring(first.offset(), closing.offset() + closing.value().length());
    }

    /** Where the run of joined tokens starting at {@code from} ends, exclusive. */
    public int joinedEnd(int from) {
        int index = from;
        while (joined(index)) {
            index++;
        }
        return index + 1;
    }

    /**
     * The first token at bracket depth zero, at or after {@code from}, that is one of these words standing whole — not a
     * part of a longer word: {@code source} is not found in {@code source-impedance}. -1 for none.
     */
    public int indexOf(int from, String... words) {
        List<String> wanted = List.of(words);
        int depth = 0;
        for (int index = from; index < tokens.size(); index++) {
            Token token = tokens.get(index);
            if (token.type() == BasicToken.T_OPEN_PAREN || token.type() == BasicToken.T_OPEN_BRACKET) {
                depth++;
            } else if (token.type() == BasicToken.T_CLOSE_PAREN || token.type() == BasicToken.T_CLOSE_BRACKET) {
                depth--;
            } else if (depth == 0 && token.type() != BasicToken.T_STRING && wanted.contains(token.value())
                    && !joined(index) && (index == 0 || !joined(index - 1))) {
                return index;
            }
        }
        return -1;
    }

    /** The commas at bracket depth zero between {@code from} and {@code to}. */
    public List<Integer> commas(int from, int to) {
        List<Integer> commas = new ArrayList<>();
        int depth = 0;
        for (int index = from; index < to; index++) {
            Token.Type type = tokens.get(index).type();
            if (type == BasicToken.T_OPEN_PAREN || type == BasicToken.T_OPEN_BRACKET) {
                depth++;
            } else if (type == BasicToken.T_CLOSE_PAREN || type == BasicToken.T_CLOSE_BRACKET) {
                depth--;
            } else if (depth == 0 && type == BasicToken.T_COMMA) {
                commas.add(index);
            }
        }
        return commas;
    }

    /** The text of tokens {@code from} up to {@code to}, as written, blanks between them included. */
    public String text(int from, int to) {
        if (from >= to) {
            return "";
        }
        int start = tokens.get(from).offset();
        int stop = to < tokens.size() ? tokens.get(to).offset() : end;
        return reader.text().substring(start, stop).strip();
    }

    public LineSyntaxException refusal(String detail) {
        return tokens.isEmpty() ? reader.refusal(detail) : reader.refusal(tokens.getFirst(), detail);
    }

    public LineSyntaxException refusal(int index, String detail) {
        if (tokens.isEmpty()) {
            return reader.refusal(detail);
        }
        return reader.refusal(tokens.get(Math.max(0, Math.min(index, tokens.size() - 1))), detail);
    }
}
