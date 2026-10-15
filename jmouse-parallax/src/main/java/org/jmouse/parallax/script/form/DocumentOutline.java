package org.jmouse.parallax.script.form;

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
 * @param kind       the document's first word
 * @param title      the title in quotes, its span without the quotes
 * @param headEnd    the offset of the brace that opens the body
 * @param bodyEnd    the offset of the brace that closes the body, or the end of the text when it is never closed
 * @param statements the body's statements, in order
 */
public record DocumentOutline(String kind, Span title, int headEnd, int bodyEnd, List<Statement> statements) {

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
        return new Scanner(text).document();
    }

    /** One pass over the text, position by position. */
    private static final class Scanner {

        private final String text;
        private int          position;

        Scanner(String text) {
            this.text = text;
        }

        DocumentOutline document() {
            skipSpaceAndComments();
            int kindStart = position;
            while (position < text.length() && isWordCharacter(text.charAt(position))) {
                position++;
            }
            String kind = text.substring(kindStart, position);
            skipSpaceAndComments();
            Span title = new Span(position, position);
            if (position < text.length() && text.charAt(position) == '"') {
                int end = endOfString(position);
                title = new Span(position + 1, Math.max(position + 1, end - 1));
                position = end;
            }
            int open = findOutsideStrings('{', position);
            if (open < 0) {
                return new DocumentOutline(kind, title, text.length(), text.length(), List.of());
            }
            position = open + 1;
            List<Statement> statements = new ArrayList<>();
            while (true) {
                skipSpaceAndComments();
                if (position >= text.length()) {
                    return new DocumentOutline(kind, title, open, text.length(), statements);
                }
                if (text.charAt(position) == '}') {
                    return new DocumentOutline(kind, title, open, position, statements);
                }
                if (text.charAt(position) == ';') {
                    position++;
                    continue;
                }
                statements.add(statement());
            }
        }

        private Statement statement() {
            int start = position;
            String indent = indentOf(start);
            int wordEnd = start;
            while (wordEnd < text.length() && isWordCharacter(text.charAt(wordEnd))) {
                wordEnd++;
            }
            String word = text.substring(start, wordEnd);
            if (wordEnd == start) {
                wordEnd = Math.min(text.length(), start + 1);
            }
            position = wordEnd;
            int headerStart = position;
            int lineEnd = endOfLine(position);
            int brace = firstOpeningBraceBefore(position, lineEnd);
            if (brace < 0) {
                position = lineEnd;
                return new Statement(word, new Span(start, contentEnd(start, lineEnd)), trimmed(headerStart, contentEnd(start, lineEnd)),
                        Optional.empty(), indent);
            }
            Span header = trimmed(headerStart, brace);
            int close = matchingBrace(brace);
            Body body = new Body(brace, close, lines(brace + 1, close));
            position = Math.min(text.length(), close + 1);
            return new Statement(word, new Span(start, position), header, Optional.of(body), indent);
        }

        /** The lines between two offsets: each ends at a line break outside braces, strings and long texts. */
        private List<Line> lines(int from, int to) {
            List<Line> lines = new ArrayList<>();
            int at = from;
            while (at < to) {
                while (at < to && (Character.isWhitespace(text.charAt(at)) || text.charAt(at) == ';')) {
                    at++;
                }
                if (at >= to) {
                    break;
                }
                if (text.charAt(at) == '#') {
                    while (at < to && text.charAt(at) != '\n') {
                        at++;
                    }
                    continue;
                }
                int end = Math.min(endOfLine(at), to);
                int content = contentEnd(at, end);
                int lineStart = Math.max(from, text.lastIndexOf('\n', at - 1) + 1);
                String before = text.substring(lineStart, at);
                int physicalStart = before.isBlank() ? lineStart : at;
                int whole = end < to && text.charAt(end) == '\n' ? end + 1 : end;
                lines.add(new Line(new Span(at, content), new Span(physicalStart, whole), before.isBlank() ? before : ""));
                at = end;
            }
            return lines;
        }

        /** The end of the line starting at an offset: its line break or {@code ;}, outside braces, strings and comments. */
        private int endOfLine(int from) {
            int depth = 0;
            int at = from;
            while (at < text.length()) {
                char character = text.charAt(at);
                if (character == '"' || character == '\'') {
                    at = endOfString(at);
                    continue;
                }
                if (character == '#') {
                    while (at < text.length() && text.charAt(at) != '\n') {
                        at++;
                    }
                    continue;
                }
                if (character == '{') {
                    depth++;
                } else if (character == '}') {
                    if (depth == 0) {
                        return at;
                    }
                    depth--;
                } else if ((character == '\n' || character == ';') && depth == 0) {
                    return at;
                }
                at++;
            }
            return text.length();
        }

        /** Where a line's content ends: before a trailing comment and trailing whitespace. */
        private int contentEnd(int from, int end) {
            int at = from;
            int last = from;
            while (at < end) {
                char character = text.charAt(at);
                if (character == '"' || character == '\'') {
                    at = Math.min(end, endOfString(at));
                    last = at;
                    continue;
                }
                if (character == '#') {
                    break;
                }
                if (!Character.isWhitespace(character)) {
                    last = at + 1;
                }
                at++;
            }
            return last;
        }

        /** The first {@code {} at depth zero between two offsets, outside strings and comments; -1 when none. */
        private int firstOpeningBraceBefore(int from, int end) {
            int at = from;
            while (at < end) {
                char character = text.charAt(at);
                if (character == '"' || character == '\'') {
                    at = endOfString(at);
                    continue;
                }
                if (character == '#') {
                    return -1;
                }
                if (character == '{') {
                    return at;
                }
                at++;
            }
            return -1;
        }

        private int matchingBrace(int open) {
            int depth = 0;
            int at = open;
            while (at < text.length()) {
                char character = text.charAt(at);
                if (character == '"' || character == '\'') {
                    at = endOfString(at);
                    continue;
                }
                if (character == '#') {
                    while (at < text.length() && text.charAt(at) != '\n') {
                        at++;
                    }
                    continue;
                }
                if (character == '{') {
                    depth++;
                } else if (character == '}') {
                    depth--;
                    if (depth == 0) {
                        return at;
                    }
                }
                at++;
            }
            return text.length();
        }

        private int findOutsideStrings(char wanted, int from) {
            int at = from;
            while (at < text.length()) {
                char character = text.charAt(at);
                if (character == '"' || character == '\'') {
                    at = endOfString(at);
                    continue;
                }
                if (character == wanted) {
                    return at;
                }
                at++;
            }
            return -1;
        }

        /** The offset just after a string that opens at {@code at}: a {@code """ … """} long text, or a quoted text. */
        private int endOfString(int at) {
            if (text.startsWith("\"\"\"", at)) {
                int end = text.indexOf("\"\"\"", at + 3);
                return end < 0 ? text.length() : end + 3;
            }
            char quote = text.charAt(at);
            int index = at + 1;
            while (index < text.length() && text.charAt(index) != quote && text.charAt(index) != '\n') {
                index++;
            }
            return Math.min(text.length(), index + 1);
        }

        private void skipSpaceAndComments() {
            while (position < text.length()) {
                char character = text.charAt(position);
                if (Character.isWhitespace(character)) {
                    position++;
                } else if (character == '#') {
                    while (position < text.length() && text.charAt(position) != '\n') {
                        position++;
                    }
                } else {
                    return;
                }
            }
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

        private static boolean isWordCharacter(char character) {
            return Character.isLetterOrDigit(character) || character == '_';
        }
    }
}
