package org.jmouse.parallax.script.legacy;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.parallax.script.legacy.ExperienceNotation.Normalized;
import org.jmouse.parallax.script.ExperienceParseException;
import org.jmouse.parallax.script.SourceText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * One reading of one text: the cursor over its tokens, the text itself, and how to say where something is wrong.
 *
 * <p>jMX is line-oriented outside its jMS bodies: a block is lines of tokens, ended by a line break, a {@code ;}, a
 * comment or a brace. Expressions inside a line are never rebuilt from tokens — {@link Line#text} cuts them out of
 * the source, so what jMF compiles is what the author wrote.</p>
 */
final class Reading {

    final TokenCursor   cursor;
    final ParserContext context;

    private final String       source;
    private final List<String> longTexts;
    private final Normalized   normalized;
    private final String       document;
    private final LineStarts   sourceLines;
    private final LineStarts   originalLines;

    Reading(TokenCursor cursor, ParserContext context, Normalized normalized, String document) {
        this.cursor = cursor;
        this.context = context;
        this.source = normalized.source();
        this.longTexts = normalized.longTexts();
        this.normalized = normalized;
        this.document = document;
        this.sourceLines = new LineStarts(source);
        this.originalLines = new LineStarts(normalized.original());
    }

    String document() {
        return document;
    }

    // ─── refusals ───────────────────────────────────────────────────────────────────────────────────

    ExperienceParseException refusal(Token at, String detail) {
        return new ExperienceParseException(document, lineOf(at), sourceLines.column(at.offset()), detail);
    }

    ExperienceParseException refusal(String detail) {
        return refusal(cursor.current(), detail);
    }

    int lineOf(Token token) {
        return sourceLines.line(token.offset());
    }

    /**
     * Where each line of a text starts, so a line and a column are found by a binary search rather than by counting
     * line breaks from the start for every expression cut out of a long document.
     */
    private static final class LineStarts {

        private final int[] starts;
        private final int   length;

        LineStarts(String text) {
            List<Integer> found = new ArrayList<>();
            found.add(0);
            for (int index = 0; index < text.length(); index++) {
                if (text.charAt(index) == '\n') {
                    found.add(index + 1);
                }
            }
            this.starts = found.stream().mapToInt(Integer::intValue).toArray();
            this.length = text.length();
        }

        /** The 1-based line an offset lies on. */
        int line(int offset) {
            return startIndex(bounded(offset)) + 1;
        }

        /** The 1-based column an offset lies at. */
        int column(int offset) {
            int bounded = bounded(offset);
            return bounded - starts[startIndex(bounded)] + 1;
        }

        private int bounded(int offset) {
            return Math.max(0, Math.min(offset, length));
        }

        /** The index of the last line start at or before an offset. */
        private int startIndex(int offset) {
            int found = Arrays.binarySearch(starts, offset);
            return found >= 0 ? found : -found - 2;
        }
    }

    // ─── text ───────────────────────────────────────────────────────────────────────────────────────

    /**
     * The text between two offsets of the normalized source, as the author wrote it — {@code x²} stays {@code x²} —
     * with where it starts in the author's text.
     */
    SourceText text(int start, int end) {
        int from = Math.max(0, Math.min(start, source.length()));
        int to = Math.max(from, Math.min(end, source.length()));
        String raw = normalized.originalBetween(from, to);
        int leading = raw.length() - raw.stripLeading().length();
        int origin = normalized.originOf(from) + leading;
        return new SourceText(raw, originalLines.line(origin), originalLines.column(origin), origin);
    }

    /** A string token's text without quotes; a {@code """ … """} placeholder becomes the long text it stands for. */
    String literal(Token token) {
        String value = token.value();
        if (value.length() >= 2 && (value.charAt(0) == '"' || value.charAt(0) == '\'')) {
            value = value.substring(1, value.length() - 1);
        }
        if (!value.isEmpty() && value.charAt(0) == ExperienceNotation.LONG_TEXT_MARK) {
            return longTexts.get(Integer.parseInt(value.substring(1)));
        }
        return value;
    }

    boolean isString(Token token) {
        return token.type() == BasicToken.T_STRING;
    }

    // ─── structure ──────────────────────────────────────────────────────────────────────────────────

    boolean atEnd() {
        return !cursor.hasNext() || cursor.isCurrent(BasicToken.T_EOL);
    }

    boolean at(BasicToken type) {
        return !atEnd() && cursor.isCurrent(type);
    }

    boolean atWord(String word) {
        return !atEnd() && cursor.current().value().equals(word) && !isString(cursor.current());
    }

    Token expect(BasicToken type, String what) {
        if (!at(type)) {
            throw refusal("expected " + what + (atEnd() ? " before the end of the document"
                    : ", found '" + cursor.current().value() + "'"));
        }
        return cursor.next();
    }

    /** Line breaks, {@code ;} and comments between declarations. */
    void skipBlank() {
        while (!atEnd()) {
            if (cursor.isCurrent(BasicToken.T_NEW_LINE, BasicToken.T_SEMICOLON, BasicToken.T_SOL)) {
                cursor.next();
            } else if (cursor.isCurrent(BasicToken.T_HASH)) {
                skipComment();
            } else {
                return;
            }
        }
    }

    private void skipComment() {
        while (!atEnd() && !cursor.isCurrent(BasicToken.T_NEW_LINE)) {
            cursor.next();
        }
    }

    /**
     * The tokens to the end of the line, stopping before a brace or a {@code ;}; a comment ends it. The cursor is left
     * on whatever stopped it.
     */
    Line line() {
        List<Token> tokens = new ArrayList<>();
        while (!atEnd() && !cursor.isCurrent(BasicToken.T_NEW_LINE, BasicToken.T_SEMICOLON,
                BasicToken.T_OPEN_CURLY, BasicToken.T_CLOSE_CURLY)) {
            if (cursor.isCurrent(BasicToken.T_HASH)) {
                int end = cursor.current().offset();
                skipComment();
                return new Line(tokens, end);
            }
            tokens.add(cursor.next());
        }
        return new Line(tokens, atEnd() ? source.length() : cursor.current().offset());
    }

    /** {@code { line … }}: every non-blank line; the cursor ends after the closing brace. Nested braces are refused. */
    List<Line> blockLines(String block) {
        expect(BasicToken.T_OPEN_CURLY, "'{' to open " + block);
        List<Line> lines = new ArrayList<>();
        while (true) {
            skipBlank();
            if (atEnd()) {
                throw refusal("'" + block + "' is opened with '{' and never closed");
            }
            if (cursor.consumeIf(BasicToken.T_CLOSE_CURLY)) {
                return lines;
            }
            if (cursor.isCurrent(BasicToken.T_OPEN_CURLY)) {
                throw refusal("'" + block + "' holds lines, not a nested '{ … }'");
            }
            Line line = line();
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }
    }

    /** The source between a block's braces, as written, for a body another language reads (jMF definitions). */
    SourceText rawBlock(String block) {
        Token open = expect(BasicToken.T_OPEN_CURLY, "'{' to open " + block);
        int depth = 1;
        while (depth > 0) {
            if (atEnd()) {
                throw refusal(open, "'" + block + "' is opened with '{' and never closed");
            }
            Token token = cursor.next();
            if (token.type() == BasicToken.T_OPEN_CURLY) {
                depth++;
            } else if (token.type() == BasicToken.T_CLOSE_CURLY) {
                depth--;
                if (depth == 0) {
                    return text(open.offset() + 1, token.offset());
                }
            }
        }
        throw refusal(open, "'" + block + "' is never closed");
    }

    /**
     * A line of tokens and the offset where it ends, so an expression at its tail is cut to the comment or the line
     * break and not to the last token's own text (a token can be shorter than what it was read from).
     */
    final class Line {

        private final List<Token> tokens;
        private final int         end;

        Line(List<Token> tokens, int end) {
            this.tokens = List.copyOf(tokens);
            this.end = end;
        }

        boolean isEmpty() {
            return tokens.isEmpty();
        }

        int size() {
            return tokens.size();
        }

        Token get(int index) {
            return tokens.get(index);
        }

        Token first() {
            return tokens.getFirst();
        }

        String value(int index) {
            return index < tokens.size() ? tokens.get(index).value() : "";
        }

        boolean isString(int index) {
            return index < tokens.size() && Reading.this.isString(tokens.get(index));
        }

        /** What ends a joined word even without a space: {@code units A, B} is two names, not {@code "A,"}. */
        private static final Set<String> SEPARATORS = Set.of(",", ";", "=", "(", ")", "[", "]", "{", "}", ":");

        /**
         * Whether token {@code index + 1} follows token {@code index} with no space between them, so that the two are
         * one word: {@code 74hc595}, {@code lm358@3}, {@code voltage-source}.
         */
        boolean joined(int index) {
            if (index + 1 >= tokens.size()) {
                return false;
            }
            String between = source.substring(tokens.get(index).offset(), tokens.get(index + 1).offset());
            return between.chars().noneMatch(Character::isWhitespace) && between.length() >= 1
                    && !SEPARATORS.contains(tokens.get(index).value()) && !SEPARATORS.contains(tokens.get(index + 1).value());
        }

        /** The run of joined tokens starting at {@code from}, as one word, and where it ends. */
        String joinedWord(int from) {
            StringBuilder word = new StringBuilder();
            int index = from;
            while (index < tokens.size()) {
                int stop = index + 1 < tokens.size() ? tokens.get(index + 1).offset() : end;
                word.append(source, tokens.get(index).offset(), stop);
                if (!joined(index)) {
                    break;
                }
                index++;
            }
            return word.toString().strip();
        }

        /** Where the run of joined tokens starting at {@code from} ends (exclusive). */
        int joinedEnd(int from) {
            int index = from;
            while (joined(index)) {
                index++;
            }
            return index + 1;
        }

        /** The words of a range of tokens, strings unquoted. */
        List<String> words(int from, int to) {
            return tokens.subList(from, to).stream()
                    .map(token -> Reading.this.isString(token) ? literal(token) : token.value()).toList();
        }

        List<String> words() {
            return words(0, tokens.size());
        }

        /** Names separated by commas from token {@code from}, each possibly hyphenated or led by digits. */
        List<String> names(int from) {
            return names(from, tokens.size());
        }

        /** Names separated by commas over tokens {@code [from, to)}. */
        List<String> names(int from, int to) {
            List<String> names = new ArrayList<>();
            int index = from;
            while (index < to) {
                if (value(index).equals(",")) {
                    index++;
                    continue;
                }
                names.add(joinedWord(index));
                index = joinedEnd(index);
            }
            return names;
        }

        /** The source text of tokens {@code [from, to)}, cut out of the source. */
        SourceText text(int from, int to) {
            if (from >= to) {
                int at = from < tokens.size() ? tokens.get(from).offset() : end;
                return Reading.this.text(at, at);
            }
            int stop = to < tokens.size() ? tokens.get(to).offset() : end;
            return Reading.this.text(tokens.get(from).offset(), stop);
        }

        SourceText text(int from) {
            return text(from, tokens.size());
        }

        /** The first token at parenthesis depth zero, at or after {@code from}, whose word is one of {@code words}. */
        int indexOf(int from, String... words) {
            List<String> wanted = List.of(words);
            int depth = 0;
            for (int index = from; index < tokens.size(); index++) {
                Token token = tokens.get(index);
                if (token.type() == BasicToken.T_OPEN_PAREN || token.type() == BasicToken.T_OPEN_BRACKET) {
                    depth++;
                } else if (token.type() == BasicToken.T_CLOSE_PAREN || token.type() == BasicToken.T_CLOSE_BRACKET) {
                    depth--;
                } else if (depth == 0 && !Reading.this.isString(token) && wanted.contains(token.value())) {
                    return index;
                }
            }
            return -1;
        }

        /** Positions of the commas at depth zero between {@code from} and {@code to}. */
        List<Integer> commas(int from, int to) {
            List<Integer> commas = new ArrayList<>();
            int depth = 0;
            for (int index = from; index < to; index++) {
                BasicToken type = tokens.get(index).type() instanceof BasicToken basic ? basic : null;
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

        /** The index of the parenthesis that closes the one at {@code open}. */
        int closing(int open) {
            int depth = 0;
            for (int index = open; index < tokens.size(); index++) {
                if (tokens.get(index).type() == BasicToken.T_OPEN_PAREN) {
                    depth++;
                } else if (tokens.get(index).type() == BasicToken.T_CLOSE_PAREN && --depth == 0) {
                    return index;
                }
            }
            throw refusal(open, "'(' is never closed on this line");
        }

        ExperienceParseException refusal(String detail) {
            return Reading.this.refusal(tokens.isEmpty() ? cursor.current() : tokens.getFirst(), detail);
        }

        ExperienceParseException refusal(int index, String detail) {
            if (tokens.isEmpty()) {
                return refusal(detail);
            }
            return Reading.this.refusal(tokens.get(Math.max(0, Math.min(index, tokens.size() - 1))), detail);
        }
    }
}
