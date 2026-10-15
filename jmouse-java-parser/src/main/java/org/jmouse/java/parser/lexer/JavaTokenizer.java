package org.jmouse.java.parser.lexer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.Tokenizer;
import org.jmouse.java.parser.source.LineIndex;
import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.ProblemKind;

/**
 * The jME {@link Tokenizer} for Java: splits a {@link JavaSourceText} into jME {@link Token}s.
 *
 * <p>It recognises everything that could otherwise fool brace and parenthesis matching: Unicode escapes
 * (translated first, as the language requires), line, block and documentation comments, string
 * literals, text blocks and character literals. Operators are left as single characters on purpose —
 * see {@link JavaTokenKind}.
 *
 * <p>Each token's {@link Token#value() value} is its text after escape translation; its
 * {@link Token#offset() offset} and {@link Token#lineNumber() line} point into the original text, and
 * its original length is recorded as the source's entry with the same ordinal. Comments are not tokens:
 * they are recorded on the source, so they can never be mistaken for code. The list ends with one
 * {@link JavaTokenKind#END_OF_FILE} token.
 *
 * <p>Tokenising never fails: a malformed literal or comment is recorded on the source as a
 * {@link ParseProblem} and closed at the most conservative point (a string at its line end, a comment or
 * text block at the end of the text), so the rest of the text is still tokenised.
 *
 * <p>Instances are stateless and safe to share.
 */
public final class JavaTokenizer implements Tokenizer<JavaSourceText, Token> {

    /** The ASCII SUB character, which the language allows as the very last character of a file. */
    private static final char END_OF_TEXT_MARKER = (char) 0x1A;

    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while");

    /**
     * Tokenises a source, recording entries, comments and problems on it.
     *
     * @param source an untokenised source
     * @return the tokens, ending with {@link JavaTokenKind#END_OF_FILE}
     */
    @Override
    public List<Token> tokenize(JavaSourceText source) {
        source.requireUntokenized();
        LineIndex          lines       = source.lines();
        List<ParseProblem> problems    = new ArrayList<>();
        var                translation = UnicodeEscapeTranslation.translate(source, lines, problems);
        Scanner            scanner     = new Scanner(source, translation, lines, problems);
        scanner.scan();
        problems.sort(Comparator.comparingInt(problem -> problem.span().start().offset()));
        problems.forEach(source::addProblem);
        scanner.comments.forEach(source::addComment);
        return List.copyOf(scanner.tokens);
    }

    /**
     * One pass over one translated text.
     */
    private static final class Scanner {

        private final JavaSourceText           source;
        private final UnicodeEscapeTranslation translation;
        private final String                   text;
        private final LineIndex                lines;
        private final List<ParseProblem>       problems;
        private final List<Token>              tokens   = new ArrayList<>();
        private final List<JavaComment>        comments = new ArrayList<>();
        private       int                      position;

        private Scanner(JavaSourceText source, UnicodeEscapeTranslation translation, LineIndex lines, List<ParseProblem> problems) {
            this.source = source;
            this.translation = translation;
            this.text = translation.text();
            this.lines = lines;
            this.problems = problems;
        }

        private void scan() {
            while (position < text.length()) {
                char character = text.charAt(position);
                if (isWhitespace(character)) {
                    position++;
                    continue;
                }
                int start = position;
                if (character == '/' && lookingAt(start + 1, '/')) {
                    scanLineComment(start);
                } else if (character == '/' && lookingAt(start + 1, '*')) {
                    scanBlockComment(start);
                } else if (character == '"') {
                    if (text.startsWith("\"\"\"", start)) {
                        scanTextBlock(start);
                    } else {
                        scanQuoted(start, '"', JavaTokenKind.STRING_LITERAL, ProblemKind.UNTERMINATED_STRING);
                    }
                } else if (character == '\'') {
                    scanQuoted(start, '\'', JavaTokenKind.CHARACTER_LITERAL, ProblemKind.UNTERMINATED_CHARACTER);
                } else if (isDigit(character) || character == '.' && position + 1 < text.length() && isDigit(text.charAt(position + 1))) {
                    scanNumber(start);
                } else if (Character.isJavaIdentifierStart(text.codePointAt(start))) {
                    scanIdentifier(start);
                } else {
                    scanPunctuation(start, character);
                }
            }
            int end = translation.originalOffset(text.length());
            record(JavaTokenKind.END_OF_FILE, "", end, end);
        }

        private void scanLineComment(int start) {
            int end = start + 2;
            while (end < text.length() && !isLineTerminator(text.charAt(end))) {
                end++;
            }
            position = end;
            addComment(JavaComment.Kind.LINE, start, end);
        }

        private void scanBlockComment(int start) {
            int     close         = text.indexOf("*/", start + 2);
            boolean documentation = text.startsWith("/**", start) && !text.startsWith("/**/", start);
            int     end;
            if (close < 0) {
                end = text.length();
                report(ProblemKind.UNTERMINATED_COMMENT, "The comment is never closed.", start, start + 2);
            } else {
                end = close + 2;
            }
            position = end;
            addComment(documentation ? JavaComment.Kind.DOCUMENTATION : JavaComment.Kind.BLOCK, start, end);
        }

        private void scanQuoted(int start, char quote, JavaTokenKind kind, ProblemKind unterminated) {
            int end = start + 1;
            while (true) {
                if (end >= text.length() || isLineTerminator(text.charAt(end))) {
                    report(unterminated, "The literal is not closed on its line.", start, start + 1);
                    break;
                }
                char character = text.charAt(end);
                if (character == '\\' && end + 1 < text.length() && !isLineTerminator(text.charAt(end + 1))) {
                    end += 2;
                    continue;
                }
                end++;
                if (character == quote) {
                    break;
                }
            }
            position = end;
            addToken(kind, start, end);
        }

        private void scanTextBlock(int start) {
            int end = start + 3;
            while (end < text.length() && (text.charAt(end) == ' ' || text.charAt(end) == '\t' || text.charAt(end) == '\f')) {
                end++;
            }
            if (end >= text.length() || !isLineTerminator(text.charAt(end))) {
                report(ProblemKind.MALFORMED_TEXT_BLOCK, "A text block opening must be followed by a line terminator.", start, start + 3);
            }
            while (true) {
                if (end >= text.length()) {
                    report(ProblemKind.UNTERMINATED_TEXT_BLOCK, "The text block is never closed.", start, start + 3);
                    break;
                }
                if (text.charAt(end) == '\\' && end + 1 < text.length()) {
                    end += 2;
                    continue;
                }
                if (text.startsWith("\"\"\"", end)) {
                    end += 3;
                    break;
                }
                end++;
            }
            position = end;
            addToken(JavaTokenKind.TEXT_BLOCK, start, end);
        }

        private void scanNumber(int start) {
            boolean hexadecimal = text.charAt(start) == '0' && start + 1 < text.length()
                    && (text.charAt(start + 1) == 'x' || text.charAt(start + 1) == 'X');
            boolean pointSeen   = false;
            int     end         = hexadecimal ? start + 2 : start;
            while (end < text.length()) {
                char character = text.charAt(end);
                if (Character.isLetterOrDigit(character) || character == '_') {
                    end++;
                } else if (character == '.' && !pointSeen && !lookingAt(end + 1, '.')) {
                    pointSeen = true;
                    end++;
                } else if ((character == '+' || character == '-') && isExponentMarker(text.charAt(end - 1), hexadecimal)) {
                    end++;
                } else {
                    break;
                }
            }
            position = end;
            addToken(JavaTokenKind.NUMBER_LITERAL, start, end);
        }

        private void scanIdentifier(int start) {
            int end = start + Character.charCount(text.codePointAt(start));
            while (end < text.length() && Character.isJavaIdentifierPart(text.codePointAt(end))) {
                end += Character.charCount(text.codePointAt(end));
            }
            position = end;
            String word = text.substring(start, end);
            JavaTokenKind kind;
            if (KEYWORDS.contains(word)) {
                kind = JavaTokenKind.KEYWORD;
            } else if (word.equals("true") || word.equals("false")) {
                kind = JavaTokenKind.BOOLEAN_LITERAL;
            } else if (word.equals("null")) {
                kind = JavaTokenKind.NULL_LITERAL;
            } else {
                kind = JavaTokenKind.IDENTIFIER;
            }
            addToken(kind, start, end);
        }

        private void scanPunctuation(int start, char character) {
            JavaTokenKind kind;
            int           end = start + 1;
            switch (character) {
                case '(' -> kind = JavaTokenKind.LEFT_PARENTHESIS;
                case ')' -> kind = JavaTokenKind.RIGHT_PARENTHESIS;
                case '{' -> kind = JavaTokenKind.LEFT_BRACE;
                case '}' -> kind = JavaTokenKind.RIGHT_BRACE;
                case '[' -> kind = JavaTokenKind.LEFT_BRACKET;
                case ']' -> kind = JavaTokenKind.RIGHT_BRACKET;
                case ';' -> kind = JavaTokenKind.SEMICOLON;
                case ',' -> kind = JavaTokenKind.COMMA;
                case '@' -> kind = JavaTokenKind.AT;
                case '<' -> kind = JavaTokenKind.LESS;
                case '>' -> kind = JavaTokenKind.GREATER;
                case '?' -> kind = JavaTokenKind.QUESTION;
                case '&' -> kind = JavaTokenKind.AMPERSAND;
                case '=' -> kind = JavaTokenKind.ASSIGN;
                case '.' -> {
                    if (text.startsWith("...", start)) {
                        kind = JavaTokenKind.ELLIPSIS;
                        end = start + 3;
                    } else {
                        kind = JavaTokenKind.DOT;
                    }
                }
                case ':' -> {
                    if (lookingAt(start + 1, ':')) {
                        kind = JavaTokenKind.DOUBLE_COLON;
                        end = start + 2;
                    } else {
                        kind = JavaTokenKind.OPERATOR;
                    }
                }
                case '+', '-', '*', '/', '%', '^', '|', '!', '~' -> kind = JavaTokenKind.OPERATOR;
                default -> {
                    kind = JavaTokenKind.UNKNOWN;
                    end = start + Character.charCount(text.codePointAt(start));
                    report(ProblemKind.UNEXPECTED_CHARACTER, "This character starts no Java token.", start, end);
                }
            }
            position = end;
            addToken(kind, start, end);
        }

        private void addToken(JavaTokenKind kind, int start, int end) {
            record(kind, text.substring(start, end), translation.originalOffset(start), translation.originalOffset(end));
        }

        /** Emits a token and its source entry under the same ordinal; the entry carries the original length. */
        private void record(JavaTokenKind kind, String value, int originalStart, int originalEnd) {
            Token token = new Token(value, kind, tokens.size(), originalStart, lines.position(originalStart).line());
            source.entry(originalStart, originalEnd - originalStart, kind);
            tokens.add(token);
        }

        private void addComment(JavaComment.Kind kind, int start, int end) {
            int originalStart = translation.originalOffset(start);
            int originalEnd   = translation.originalOffset(end);
            comments.add(new JavaComment(kind, source.subSequence(originalStart, originalEnd).toString(), lines.span(originalStart, originalEnd)));
        }

        private void report(ProblemKind kind, String message, int start, int end) {
            problems.add(new ParseProblem(kind, message, lines.span(translation.originalOffset(start), translation.originalOffset(end))));
        }

        private boolean lookingAt(int index, char expected) {
            return index < text.length() && text.charAt(index) == expected;
        }

        private static boolean isExponentMarker(char previous, boolean hexadecimal) {
            if (hexadecimal) {
                return previous == 'p' || previous == 'P';
            }
            return previous == 'e' || previous == 'E';
        }

        private static boolean isDigit(char character) {
            return character >= '0' && character <= '9';
        }

        private static boolean isLineTerminator(char character) {
            return character == '\n' || character == '\r';
        }

        private static boolean isWhitespace(char character) {
            return character == ' ' || character == '\t' || character == '\f' || character == '\n' || character == '\r'
                    || character == END_OF_TEXT_MARKER;
        }
    }
}
