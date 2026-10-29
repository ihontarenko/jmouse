package org.jmouse.gerber.lexer;

import org.jmouse.el.lexer.RawToken;
import org.jmouse.el.lexer.Splitter;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.gerber.GerberParseException;
import org.jmouse.gerber.GerberPosition;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a Gerber text into jME {@link RawToken}s: the lexical rules of Gerber, settled once, so that nothing after the
 * splitter knows what a block is.
 *
 * <ul>
 *     <li><b>Blocks.</b> A block ends with {@code *}; line breaks mean nothing anywhere and are dropped.</li>
 *     <li><b>Extended commands.</b> Between two {@code %}, each block's body is one token as written —
 *     {@code ADD10C,0.25}, {@code TF.FileFunction,Copper,L1,Top} —: its grammar is its command's.</li>
 *     <li><b>Word commands.</b> Outside them, a block is words: a letter and the number after it —
 *     {@code X-1500Y200D01} is four. {@code G04} is followed by its comment as one token, whatever it says.</li>
 * </ul>
 *
 * <p>jME's {@link RawToken.Type} names what was found, and the {@link GerberTokenizer} turns each into a
 * {@link GerberTokenKind}: {@code OPERATOR} is {@code %}, {@code IDENTIFIER} a word, {@code STRING} a body or a
 * comment, {@code NEW_LINE} the end of a block, {@code UNKNOWN} a character Gerber has no use for.</p>
 */
public final class GerberSplitter implements Splitter<List<RawToken>, TokenizableSource> {

    private static final String COMMENT = "G04";

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
        private int     index;
        private int     line = 1;
        private boolean extended;

        Scan(TokenizableSource source, int start, int end) {
            this.source = source;
            this.index = start;
            this.end = end;
        }

        void run() {
            while (index < end) {
                char character = source.charAt(index);
                if (character == '\n') {
                    line++;
                    index++;
                } else if (Character.isWhitespace(character)) {
                    index++;
                } else if (character == '%') {
                    tokens.add(new RawToken("%", line, index, RawToken.Type.OPERATOR));
                    extended = !extended;
                    index++;
                } else if (extended) {
                    extendedBlock();
                } else {
                    wordBlock();
                }
            }
            if (extended) {
                throw new GerberParseException(new GerberPosition(line, 0), "an extended command is never closed by '%'");
            }
        }

        /** One block of an extended command: its body as written, line breaks taken out, then its end. */
        private void extendedBlock() {
            int start = index;
            int startLine = line;
            StringBuilder body = new StringBuilder();
            while (index < end && source.charAt(index) != '*') {
                char character = source.charAt(index);
                if (character == '%') {
                    throw refusal(startLine, start, "a block of an extended command ends with '*' before its '%'");
                }
                if (character == '\n') {
                    line++;
                } else if (character != '\r') {
                    body.append(character);
                }
                index++;
            }
            endOfBlock(start, startLine);
            tokens.add(new RawToken(body.toString(), startLine, start, RawToken.Type.STRING));
            tokens.add(new RawToken("*", line, index, RawToken.Type.NEW_LINE));
            index++;
        }

        /** One block of words; a comment's words are its text. */
        private void wordBlock() {
            int start = index;
            int startLine = line;
            if (startsWith(COMMENT)) {
                tokens.add(new RawToken(COMMENT, line, index, RawToken.Type.IDENTIFIER));
                index += COMMENT.length();
                int textStart = index;
                while (index < end && source.charAt(index) != '*') {
                    if (source.charAt(index) == '\n') {
                        line++;
                    }
                    index++;
                }
                endOfBlock(start, startLine);
                String text = source.subSequence(textStart, index).toString().replaceAll("[\\r\\n]", "").strip();
                tokens.add(new RawToken(text, startLine, textStart, RawToken.Type.STRING));
            } else {
                while (index < end && source.charAt(index) != '*') {
                    char character = source.charAt(index);
                    if (character == '\n') {
                        line++;
                        index++;
                    } else if (Character.isWhitespace(character)) {
                        index++;
                    } else if (Character.isLetter(character)) {
                        word();
                    } else {
                        tokens.add(new RawToken(String.valueOf(character), line, index, RawToken.Type.UNKNOWN));
                        index++;
                    }
                }
                endOfBlock(start, startLine);
            }
            tokens.add(new RawToken("*", line, index, RawToken.Type.NEW_LINE));
            index++;
        }

        /** A letter and the number after it: a sign, digits, a point. */
        private void word() {
            int start = index;
            index++;
            while (index < end && isNumberCharacter(source.charAt(index))) {
                index++;
            }
            tokens.add(new RawToken(source.subSequence(start, index).toString(), line, start, RawToken.Type.IDENTIFIER));
        }

        private static boolean isNumberCharacter(char character) {
            return Character.isDigit(character) || character == '-' || character == '+' || character == '.';
        }

        private boolean startsWith(String word) {
            if (index + word.length() > end || !source.subSequence(index, index + word.length()).toString().equals(word)) {
                return false;
            }
            // G04 and not G040: a comment's code ends where its digits do.
            return index + word.length() >= end || !Character.isDigit(source.charAt(index + word.length()));
        }

        private void endOfBlock(int start, int startLine) {
            if (index >= end) {
                throw refusal(startLine, start, "a block never ends with '*'");
            }
        }

        private GerberParseException refusal(int atLine, int offset, String detail) {
            return new GerberParseException(new GerberPosition(atLine, source.getColumnNumber(offset)), detail);
        }
    }
}
