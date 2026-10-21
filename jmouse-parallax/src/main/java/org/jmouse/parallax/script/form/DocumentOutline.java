package org.jmouse.parallax.script.form;

import org.jmouse.el.StringSource;
import org.jmouse.el.lexer.ExpressionSplitter;
import org.jmouse.el.lexer.RawToken;
import org.jmouse.parallax.script.ExperienceReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Where everything of a jMX text is: the document's head, each statement of its body, the lines inside each
 * statement's braces — as offsets into the text exactly as the author wrote it.
 *
 * <p>This is the text's <em>shape</em>, not its grammar: strings, {@code """ … """} text, {@code #} comments, braces and
 * line breaks. It is what lets a form change one value by patching one span, so comments, order and formatting
 * survive an edit. What a line means is the reader's to say ({@link LineCodecs}); the outline only says where it is,
 * and it is found even in a text the reader refuses, so a form can still show the document it cannot read.</p>
 *
 * <p>The shape is read from the engine's tokens — jMX's dialect with its long texts and its {@code #} comments —, so a
 * string, a long text and a comment are exactly what the reader takes them to be. ⚠️ One rule is the outline's own,
 * because a text being edited is often broken: a quote that is not closed on its line runs to the end of that line.</p>
 *
 * @param kind       the document's first word
 * @param title      the title in quotes, its span without the quotes
 * @param headEnd    the offset of the brace that opens the body
 * @param bodyEnd    the offset of the brace that closes the body, or the end of the text when it is never closed
 * @param statements the body's statements, in order
 */
public record DocumentOutline(String kind, Span title, int headEnd, int bodyEnd, List<Statement> statements) {

    /** jMX's lexing, with the two things a document's text has that an expression has not. */
    private static final ExpressionSplitter SPLITTER = new ExpressionSplitter(
            ExperienceReader.DIALECT.withLongTexts().withLineComments());

    public DocumentOutline {
        statements = List.copyOf(statements);
    }

    /** A range of the text: {@code [start, end)}. */
    public record Span(int start, int end) {

        public String of(String text) {
            return text.substring(start, end);
        }
    }

    /**
     * One statement of the body: {@code parameters { … }}, {@code symbol uses resistor-symbol { a one }}, {@code uses
     * lm358}, {@code pin a at -30, 0}.
     *
     * @param word   its first word
     * @param span   from its first word to the end of its closing brace, or of its line
     * @param header what follows the word up to the opening brace or the end of the line, trimmed
     * @param body   the lines between its braces, when it has braces
     * @param indent the whitespace its line starts with
     */
    public record Statement(String word, Span span, Span header, Optional<Body> body, String indent) {
    }

    /**
     * @param open  the offset of the opening brace
     * @param close the offset of the closing brace
     * @param lines every line between them that holds more than a comment
     */
    public record Body(int open, int close, List<Line> lines) {

        public Body {
            lines = List.copyOf(lines);
        }
    }

    /**
     * One line of a body: what an edit replaces is {@link #content()}; its indentation and a comment after it stay.
     *
     * @param content from its first character to the end of its last, before a trailing comment
     * @param whole   the whole physical line or lines, line break included — what a deletion removes
     * @param indent  the whitespace the line starts with
     */
    public record Line(Span content, Span whole, String indent) {
    }

    // ─── reading the shape ──────────────────────────────────────────────────────────────────────────

    public static DocumentOutline of(String text) {
        return new Shape(text, pieces(text)).document();
    }

    /** What a piece of the text is to its shape. */
    private enum PieceKind { WORD, STRING, OPEN, CLOSE, SEPARATOR, LINE_BREAK, OTHER }

    /** One piece of the text: a token, or a line break; comments and space between them are not pieces. */
    private record Piece(PieceKind kind, int start, int end) {
    }

    /**
     * The text's tokens as pieces of its shape. A line-break token stands for several breaks, and a line ends at each.
     * A quote the lexer left alone — not closed on its line — is a string to the end of that line, its break included,
     * and whatever was lexed inside it is passed over.
     */
    private static List<Piece> pieces(String text) {
        List<Piece> pieces = new ArrayList<>();
        int passedUntil = 0;
        for (RawToken token : SPLITTER.split(new StringSource("outline", text))) {
            int start = token.offset();
            int end = start + token.length();
            if (token.type() == RawToken.Type.NEW_LINE) {
                for (int at = Math.max(start, passedUntil); at < end; at++) {
                    if (text.charAt(at) == '\n') {
                        pieces.add(new Piece(PieceKind.LINE_BREAK, at, at + 1));
                    }
                }
                continue;
            }
            if (start < passedUntil) {
                continue;
            }
            String value = token.value();
            PieceKind kind = switch (token.type()) {
                case STRING -> PieceKind.STRING;
                case IDENTIFIER, NUMBER -> PieceKind.WORD;
                default -> switch (value) {
                    case "{" -> PieceKind.OPEN;
                    case "}" -> PieceKind.CLOSE;
                    case ";" -> PieceKind.SEPARATOR;
                    case "\"", "'" -> PieceKind.STRING;
                    default -> PieceKind.OTHER;
                };
            };
            if (kind == PieceKind.STRING && (value.equals("\"") || value.equals("'"))) {
                int lineBreak = text.indexOf('\n', start);
                end = lineBreak < 0 ? text.length() : lineBreak + 1;
                passedUntil = end;
            }
            pieces.add(new Piece(kind, start, end));
        }
        return pieces;
    }

    /** One pass over the pieces, in order. */
    private static final class Shape {

        private final String      text;
        private final List<Piece> pieces;
        private int               index;

        Shape(String text, List<Piece> pieces) {
            this.text = text;
            this.pieces = pieces;
        }

        DocumentOutline document() {
            skipLineBreaks();
            int kindStart = startOf(index);
            String kind = wordAt(kindStart);
            index = firstFrom(kindStart + kind.length());
            skipLineBreaks();
            Span title = new Span(startOf(index), startOf(index));
            if (at(PieceKind.STRING) && text.charAt(startOf(index)) == '"') {
                Piece string = pieces.get(index);
                title = new Span(string.start() + 1, Math.max(string.start() + 1, string.end() - 1));
                index++;
            }
            int open = next(PieceKind.OPEN, index);
            if (open < 0) {
                return new DocumentOutline(kind, title, text.length(), text.length(), List.of());
            }
            int headEnd = pieces.get(open).start();
            index = open + 1;
            List<Statement> statements = new ArrayList<>();
            while (true) {
                skipLineBreaks();
                if (index >= pieces.size()) {
                    return new DocumentOutline(kind, title, headEnd, text.length(), statements);
                }
                if (at(PieceKind.CLOSE)) {
                    return new DocumentOutline(kind, title, headEnd, startOf(index), statements);
                }
                if (at(PieceKind.SEPARATOR)) {
                    index++;
                    continue;
                }
                statements.add(statement());
            }
        }

        private Statement statement() {
            int start = startOf(index);
            String indent = indentOf(start);
            String word = wordAt(start);
            int wordEnd = word.isEmpty() ? Math.min(text.length(), start + 1) : start + word.length();
            int headerStart = wordEnd;
            int first = firstFrom(wordEnd);
            int lineEnd = endOfLine(first);
            int brace = openingBraceBefore(first, lineEnd);
            if (brace < 0) {
                index = firstFrom(lineEnd);
                // The word itself is content: a statement of one word (`end`) ends where the word does.
                int end = contentEnd(first, lineEnd, wordEnd);
                return new Statement(word, new Span(start, end), trimmed(headerStart, end), Optional.empty(), indent);
            }
            int openOffset = pieces.get(brace).start();
            Span header = trimmed(headerStart, openOffset);
            int close = matchingBrace(brace);
            int closeOffset = close < 0 ? text.length() : pieces.get(close).start();
            Body body = new Body(openOffset, closeOffset, lines(brace + 1, closeOffset));
            int after = Math.min(text.length(), closeOffset + 1);
            index = close < 0 ? pieces.size() : close + 1;
            return new Statement(word, new Span(start, after), header, Optional.of(body), indent);
        }

        /** The lines between a piece and an offset: each ends at a line break or {@code ;} outside braces. */
        private List<Line> lines(int fromPiece, int to) {
            List<Line> lines = new ArrayList<>();
            int at = fromPiece;
            while (at < pieces.size() && startOf(at) < to) {
                if (pieces.get(at).kind() == PieceKind.LINE_BREAK || pieces.get(at).kind() == PieceKind.SEPARATOR) {
                    at++;
                    continue;
                }
                int start = startOf(at);
                int end = Math.min(endOfLine(at), to);
                int content = contentEnd(at, end, start);
                int lineStart = text.lastIndexOf('\n', start - 1) + 1;
                String before = text.substring(lineStart, start);
                int physicalStart = before.isBlank() ? lineStart : start;
                int whole = end < to && text.charAt(end) == '\n' ? end + 1 : end;
                lines.add(new Line(new Span(start, content), new Span(physicalStart, whole), before.isBlank() ? before : ""));
                at = firstFrom(end);
            }
            return lines;
        }

        /** Where the line holding a piece ends: its line break or {@code ;} at depth zero, or a closing brace above it. */
        private int endOfLine(int fromPiece) {
            int depth = 0;
            for (int at = fromPiece; at < pieces.size(); at++) {
                Piece piece = pieces.get(at);
                switch (piece.kind()) {
                    case OPEN -> depth++;
                    case CLOSE -> {
                        if (depth == 0) {
                            return piece.start();
                        }
                        depth--;
                    }
                    case LINE_BREAK, SEPARATOR -> {
                        if (depth == 0) {
                            return piece.start();
                        }
                    }
                    default -> {
                    }
                }
            }
            return text.length();
        }

        /** Where a line's content ends: after its last piece before an offset — a trailing comment is no piece. */
        private int contentEnd(int fromPiece, int end, int start) {
            int last = start;
            for (int at = fromPiece; at < pieces.size() && startOf(at) < end; at++) {
                if (pieces.get(at).kind() != PieceKind.LINE_BREAK) {
                    last = Math.min(end, pieces.get(at).end());
                }
            }
            return last;
        }

        /** The first opening brace among the pieces before an offset; -1 when none. */
        private int openingBraceBefore(int fromPiece, int end) {
            for (int at = fromPiece; at < pieces.size() && startOf(at) < end; at++) {
                if (pieces.get(at).kind() == PieceKind.OPEN) {
                    return at;
                }
            }
            return -1;
        }

        /** The brace that closes the one at a piece; -1 when the text ends first. */
        private int matchingBrace(int open) {
            int depth = 0;
            for (int at = open; at < pieces.size(); at++) {
                if (pieces.get(at).kind() == PieceKind.OPEN) {
                    depth++;
                } else if (pieces.get(at).kind() == PieceKind.CLOSE) {
                    depth--;
                    if (depth == 0) {
                        return at;
                    }
                }
            }
            return -1;
        }

        private int next(PieceKind kind, int fromPiece) {
            for (int at = fromPiece; at < pieces.size(); at++) {
                if (pieces.get(at).kind() == kind) {
                    return at;
                }
            }
            return -1;
        }

        /** The first piece that starts at or after an offset. */
        private int firstFrom(int offset) {
            int at = 0;
            while (at < pieces.size() && pieces.get(at).start() < offset) {
                at++;
            }
            return at;
        }

        private boolean at(PieceKind kind) {
            return index < pieces.size() && pieces.get(index).kind() == kind;
        }

        private int startOf(int piece) {
            return piece < pieces.size() ? pieces.get(piece).start() : text.length();
        }

        private void skipLineBreaks() {
            while (at(PieceKind.LINE_BREAK)) {
                index++;
            }
        }

        /** The word a statement or the document opens with: the letters, digits and underscores at an offset. */
        private String wordAt(int offset) {
            int end = offset;
            while (end < text.length() && (Character.isLetterOrDigit(text.charAt(end)) || text.charAt(end) == '_')) {
                end++;
            }
            return text.substring(offset, end);
        }

        private Span trimmed(int from, int to) {
            int start = from;
            int end = Math.max(from, to);
            while (start < end && Character.isWhitespace(text.charAt(start))) {
                start++;
            }
            while (end > start && Character.isWhitespace(text.charAt(end - 1))) {
                end--;
            }
            return new Span(start, end);
        }

        private String indentOf(int offset) {
            int lineStart = text.lastIndexOf('\n', offset - 1) + 1;
            String before = text.substring(lineStart, offset);
            return before.isBlank() ? before : "";
        }
    }
}
