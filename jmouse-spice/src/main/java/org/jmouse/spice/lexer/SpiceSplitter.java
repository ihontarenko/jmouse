package org.jmouse.spice.lexer;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Splitter;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.spice.SpiceParseException;
import org.jmouse.spice.SpicePosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Splits a SPICE text into jME {@link RawToken}s: the lexical rules of SPICE, settled once, so that nothing after the
 * splitter knows what a line is.
 *
 * <ul>
 *     <li><b>The title.</b> SPICE reads the first line as the deck's title, whatever it says. A text whose first line is
 *     a dot card — a library file, a pasted {@code .subckt} — has none: that line is a card.</li>
 *     <li><b>Comments.</b> A line whose first character is {@code *} or {@code ;} is a comment; so is everything after
 *     {@code ;}, and after a {@code $} that follows a blank, outside braces and quotes. Comments are kept, as tokens,
 *     for the parser to hang on the nodes as trivia.</li>
 *     <li><b>Cards.</b> A card ends where its line ends, unless the next line begins with {@code +}; the end is a raw
 *     token of its own. Blank lines and comment lines between a card and its continuation do not end it.</li>
 *     <li><b>Expressions.</b> {@code {…}} (braces nested) and {@code '…'} are one token each, and so is {@code "…"}, a
 *     text; none may run past the end of its line.</li>
 *     <li><b>Scripts.</b> The lines between {@code .control} and {@code .endc} are SPICE's interpreter's, not the
 *     netlist's: each is one raw token, as written.</li>
 * </ul>
 *
 * <h2>How SPICE's tokens travel as raw tokens</h2>
 *
 * <p>jME's {@link RawToken.Type} names what a splitter found, and the {@link SpiceTokenizer} turns each into a
 * {@link SpiceTokenKind}: {@code OPEN_TAG} is the title, {@code IDENTIFIER} a word (a number with its scale factor is a
 * word too), {@code STRING} an expression or a text with its delimiters, {@code OPERATOR} one of {@code ( ) , =},
 * {@code RAW_TEXT} a comment, {@code CLOSE_TAG} a line of a {@code .control} script, {@code NEW_LINE} the end of a card,
 * and {@code UNKNOWN} a character SPICE has no use for.</p>
 */
public final class SpiceSplitter implements Splitter<List<RawToken>, TokenizableSource> {

    @Override
    public List<RawToken> split(TokenizableSource source, int offset, int length) {
        Scan scan = new Scan(source, offset, offset + length);
        scan.run();
        return scan.tokens;
    }

    /** One pass over one text. */
    private static final class Scan {

        private final TokenizableSource source;
        private final int               end;
        private final List<RawToken>    tokens = new ArrayList<>();
        private int     lineStart;
        private int     line = 1;
        private boolean cardOpen;
        private int     cardEnd;
        private int     cardEndLine;
        private boolean control;
        /** Comment lines met while a card is open: they belong between its lines if a continuation follows, else after it. */
        private final List<RawToken> waitingComments = new ArrayList<>();

        Scan(TokenizableSource source, int start, int end) {
            this.source = source;
            this.end = end;
            this.lineStart = start;
        }

        void run() {
            boolean first = true;
            while (lineStart < end) {
                int lineEnd = lineStart;
                while (lineEnd < end && source.charAt(lineEnd) != '\n' && source.charAt(lineEnd) != '\r') {
                    lineEnd++;
                }
                if (first) {
                    title(lineStart, lineEnd);
                    first = false;
                } else {
                    line(lineStart, lineEnd);
                }
                lineStart = nextLine(lineEnd);
                line++;
            }
            closeCard();
            flushComments();
        }

        private int nextLine(int lineEnd) {
            if (lineEnd < end && source.charAt(lineEnd) == '\r' && lineEnd + 1 < end && source.charAt(lineEnd + 1) == '\n') {
                return lineEnd + 2;
            }
            return lineEnd + 1;
        }

        /** The first line: the title, unless it is a dot card. */
        private void title(int start, int lineEnd) {
            int content = skipBlanks(start, lineEnd);
            if (content < lineEnd && source.charAt(content) == '.') {
                line(start, lineEnd);
                return;
            }
            tokens.add(new RawToken(text(content, trimmedEnd(content, lineEnd)), line, content, RawToken.Type.OPEN_TAG));
        }

        private void line(int start, int lineEnd) {
            int content = skipBlanks(start, lineEnd);
            if (control) {
                if (startsWithWord(content, lineEnd, ".endc")) {
                    control = false;
                    card(content, lineEnd);
                } else {
                    tokens.add(new RawToken(text(start, lineEnd), line, start, RawToken.Type.CLOSE_TAG));
                }
                return;
            }
            if (content >= lineEnd) {
                return;
            }
            char first = source.charAt(content);
            if (first == '*' || first == ';') {
                (cardOpen ? waitingComments : tokens).add(new RawToken(text(content, trimmedEnd(content, lineEnd)), line, content, RawToken.Type.RAW_TEXT));
                return;
            }
            if (first == '+') {
                if (!cardOpen) {
                    throw new SpiceParseException(new SpicePosition(line, content - start + 1),
                            "a line that begins with '+' carries on the card above it, and there is none");
                }
                flushComments();
                words(content + 1, lineEnd, start);
                return;
            }
            closeCard();
            flushComments();
            card(content, lineEnd);
            if (startsWithWord(content, lineEnd, ".control")) {
                control = true;
                closeCard();
            }
        }

        private void card(int content, int lineEnd) {
            cardOpen = true;
            cardEnd = content;
            cardEndLine = line;
            words(content, lineEnd, lineStart);
        }

        private void flushComments() {
            tokens.addAll(waitingComments);
            waitingComments.clear();
        }

        private void closeCard() {
            if (cardOpen) {
                tokens.add(new RawToken("", cardEndLine, cardEnd, RawToken.Type.NEW_LINE));
                cardOpen = false;
            }
        }

        /** The tokens of one physical line from {@code from}, up to its end or its comment. */
        private void words(int from, int lineEnd, int start) {
            int index = from;
            while (index < lineEnd) {
                char character = source.charAt(index);
                if (Character.isWhitespace(character)) {
                    index++;
                    continue;
                }
                if (character == ';' || character == '$' && Character.isWhitespace(source.charAt(index - 1))) {
                    tokens.add(new RawToken(text(index, trimmedEnd(index, lineEnd)), line, index, RawToken.Type.RAW_TEXT));
                    break;
                }
                int close = switch (character) {
                    case '(', ')', ',', '=' -> index + 1;
                    case '{' -> closingBrace(index, lineEnd, start) + 1;
                    case '\'', '"' -> closingQuote(index, lineEnd, start) + 1;
                    case '}' -> index + 1;
                    default -> wordEnd(index, lineEnd);
                };
                RawToken.Type type = switch (character) {
                    case '(', ')', ',', '=' -> RawToken.Type.OPERATOR;
                    case '{', '\'', '"' -> RawToken.Type.STRING;
                    case '}' -> RawToken.Type.UNKNOWN;
                    default -> RawToken.Type.IDENTIFIER;
                };
                tokens.add(new RawToken(text(index, close), line, index, type));
                index = close;
                cardEnd = index;
                cardEndLine = line;
            }
        }

        private int wordEnd(int from, int lineEnd) {
            int index = from;
            while (index < lineEnd && !endsWord(source.charAt(index))) {
                index++;
            }
            return index;
        }

        private static boolean endsWord(char character) {
            return Character.isWhitespace(character) || "(),={};'\"".indexOf(character) >= 0;
        }

        private int closingBrace(int open, int lineEnd, int start) {
            int depth = 0;
            for (int index = open; index < lineEnd; index++) {
                char character = source.charAt(index);
                if (character == '{') {
                    depth++;
                } else if (character == '}') {
                    depth--;
                    if (depth == 0) {
                        return index;
                    }
                }
            }
            throw new SpiceParseException(new SpicePosition(line, open - start + 1),
                    "the '{' opened here never closes on this line");
        }

        private int closingQuote(int open, int lineEnd, int start) {
            char quote = source.charAt(open);
            for (int index = open + 1; index < lineEnd; index++) {
                if (source.charAt(index) == quote) {
                    return index;
                }
            }
            throw new SpiceParseException(new SpicePosition(line, open - start + 1),
                    "the quote opened here never closes on this line");
        }

        private boolean startsWithWord(int content, int lineEnd, String word) {
            int wordEnd = wordEnd(content, lineEnd);
            return text(content, wordEnd).toLowerCase(Locale.ROOT).equals(word);
        }

        private int skipBlanks(int from, int lineEnd) {
            int index = from;
            while (index < lineEnd && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
            return index;
        }

        private int trimmedEnd(int from, int lineEnd) {
            int index = lineEnd;
            while (index > from && Character.isWhitespace(source.charAt(index - 1))) {
                index--;
            }
            return index;
        }

        private String text(int from, int to) {
            return source.subSequence(from, to).toString();
        }
    }
}
