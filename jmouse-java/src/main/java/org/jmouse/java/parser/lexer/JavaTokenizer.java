package org.jmouse.java.parser.lexer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jmouse.java.model.version.JavaSourceProfile;
import org.jmouse.java.model.version.JavaSpecificationCatalog;

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
 * see {@link JavaToken}.
 *
 * <p>Each token's {@link Token#value() value} is its text after escape translation; its
 * {@link Token#offset() offset} and {@link Token#lineNumber() line} point into the original text, and
 * its original length is recorded as the source's entry with the same ordinal. Comments are not tokens:
 * they are recorded on the source, so they can never be mistaken for code. The list ends with one
 * {@link JavaToken#END_OF_FILE} token.
 *
 * <p>Tokenising never fails: a malformed literal or comment is recorded on the source as a
 * {@link ParseProblem} and closed at the most conservative point (a string at its line end, a comment or
 * text block at the end of the text), so the rest of the text is still tokenised.
 *
 * <p>Instances are stateless and safe to share.
 */
public final class JavaTokenizer implements Tokenizer<JavaSourceText, Token> {
    private final JavaLexicalProfile lexical;
    private final JavaSourceProfile profile;

    public JavaTokenizer() {
        this(JavaSourceProfile.standard(JavaSpecificationCatalog.standard().defaultRelease()), JavaLexicalProfile.standard());
    }

    public JavaTokenizer(JavaSourceProfile profile, JavaLexicalProfile lexical) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.lexical = Objects.requireNonNull(lexical, "lexical");
    }

    /** The ASCII SUB character, which the language allows as the very last character of a file. */
    private static final char END_OF_TEXT_MARKER = (char) 0x1A;


    /**
     * Tokenises a source, recording entries, comments and problems on it.
     *
     * @param source an untokenised source
     * @return the tokens, ending with {@link JavaToken#END_OF_FILE}
     */
    @Override
    public List<Token> tokenize(JavaSourceText source) {
        source.requireUntokenized();
        LineIndex          lines       = source.lines();
        List<ParseProblem> problems    = new ArrayList<>();
        UnicodeEscapeTranslation translation = UnicodeEscapeTranslation.translate(source, lines, problems);
        Scanner            scanner     = new Scanner(source, translation, lines, problems, profile, lexical);
        scanner.scan();
        problems.sort(Comparator.comparingInt(problem -> problem.span().start().offset()));
        problems.forEach(source::addProblem);
        scanner.comments.stream().sorted(Comparator.comparingInt(comment -> comment.span().start().offset()))
                .forEach(source::addComment);
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
        private final JavaSourceProfile profile;
        private final JavaLexicalProfile lexical;
        private       int                      position;

        private Scanner(JavaSourceText source, UnicodeEscapeTranslation translation, LineIndex lines, List<ParseProblem> problems,
                        JavaSourceProfile profile, JavaLexicalProfile lexical) {
            this.source = source;
            this.translation = translation;
            this.text = translation.text();
            this.lines = lines;
            this.problems = problems;
            this.profile = profile;
            this.lexical = lexical;
        }

        private void scan() {
            while (position < text.length()) {
                char character = text.charAt(position);
                if (isWhitespace(character)) {
                    position++;
                    continue;
                }
                int start = position;
                Optional<JavaLexicalMatch> extension = lexical.recognize(new JavaLexicalInput(text, start, profile));
                if (extension.isPresent()) {
                    contribution(extension.get());
                    continue;
                }
                if (character == '/' && lookingAt(start + 1, '/')) {
                    scanLineComment(start);
                } else if (character == '/' && lookingAt(start + 1, '*')) {
                    scanBlockComment(start);
                } else if (character == '"') {
                    if (text.startsWith("\"\"\"", start)) {
                        scanTextBlock(start);
                    } else {
                        scanQuoted(start, '"', JavaToken.STRING_LITERAL, ProblemKind.UNTERMINATED_STRING);
                    }
                } else if (character == '\'') {
                    scanQuoted(start, '\'', JavaToken.CHARACTER_LITERAL, ProblemKind.UNTERMINATED_CHARACTER);
                } else if (isDigit(character) || character == '.' && position + 1 < text.length() && isDigit(text.charAt(position + 1))) {
                    scanNumber(start);
                } else if (lexical.isIdentifierStart(text.codePointAt(start))) {
                    scanIdentifier(start);
                } else {
                    scanPunctuation(start, character);
                }
            }
            int end = translation.originalOffset(text.length());
            record(JavaToken.END_OF_FILE, "", end, end);
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

        private void scanQuoted(int start, char quote, JavaToken kind, ProblemKind unterminated) {
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
            addToken(JavaToken.TEXT_BLOCK, start, end);
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
            addToken(JavaToken.NUMBER_LITERAL, start, end);
        }

        private void scanIdentifier(int start) {
            int end = start + Character.charCount(text.codePointAt(start));
            while (end < text.length() && lexical.isIdentifierPart(text.codePointAt(end))) {
                end += Character.charCount(text.codePointAt(end));
            }
            position = end;
            String word = text.substring(start, end);
            JavaToken kind;
            if (lexical.keywords().isReserved(word)) {
                kind = JavaToken.KEYWORD;
            } else if (word.equals("true") || word.equals("false")) {
                kind = JavaToken.BOOLEAN_LITERAL;
            } else if (word.equals("null")) {
                kind = JavaToken.NULL_LITERAL;
            } else {
                kind = JavaToken.IDENTIFIER;
            }
            addToken(kind, start, end);
        }

        private void scanPunctuation(int start, char character) {
            JavaToken kind;
            int           end = start + 1;
            switch (character) {
                case '(' -> kind = JavaToken.LEFT_PARENTHESIS;
                case ')' -> kind = JavaToken.RIGHT_PARENTHESIS;
                case '{' -> kind = JavaToken.LEFT_BRACE;
                case '}' -> kind = JavaToken.RIGHT_BRACE;
                case '[' -> kind = JavaToken.LEFT_BRACKET;
                case ']' -> kind = JavaToken.RIGHT_BRACKET;
                case ';' -> kind = JavaToken.SEMICOLON;
                case ',' -> kind = JavaToken.COMMA;
                case '@' -> kind = JavaToken.AT;
                case '<' -> kind = JavaToken.LESS;
                case '>' -> kind = JavaToken.GREATER;
                case '?' -> kind = JavaToken.QUESTION;
                case '&' -> kind = JavaToken.AMPERSAND;
                case '=' -> kind = JavaToken.ASSIGN;
                case '.' -> {
                    if (text.startsWith("...", start)) {
                        kind = JavaToken.ELLIPSIS;
                        end = start + 3;
                    } else {
                        kind = JavaToken.DOT;
                    }
                }
                case ':' -> {
                    if (lookingAt(start + 1, ':')) {
                        kind = JavaToken.DOUBLE_COLON;
                        end = start + 2;
                    } else {
                        kind = JavaToken.OPERATOR;
                    }
                }
                case '+', '-', '*', '/', '%', '^', '|', '!', '~' -> kind = JavaToken.OPERATOR;
                default -> {
                    kind = JavaToken.UNKNOWN;
                    end = start + Character.charCount(text.codePointAt(start));
                    report(ProblemKind.UNEXPECTED_CHARACTER, "This character starts no Java token.", start, end);
                }
            }
            position = end;
            addToken(kind, start, end);
        }

        private void addToken(JavaToken kind, int start, int end) {
            record(kind, text.substring(start, end), translation.originalOffset(start), translation.originalOffset(end));
        }

        /** Emits a token and its source entry under the same ordinal; the entry carries the original length. */
        private void record(Token.Type kind, String value, int originalStart, int originalEnd) {
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
            problems.add(new ParseProblem(kind, message, lines.span(translation.originalOffset(start), translation.originalOffset(end)),
                    ParseProblem.Phase.LEXICAL));
        }

        private void contribution(JavaLexicalMatch match) {
            int start = position;
            int end = Math.addExact(start, match.length());
            Objects.checkFromToIndex(start, end, text.length());
            ScopedOutput output = new ScopedOutput(start, end);
            try {
                match.emission().accept(output);
            } finally {
                output.state = OutputState.CLOSED;
            }
            position = end;
        }

        private enum OutputState { OPEN, CLOSED }

        /** The callback cannot retain an active emitter or write outside its recognized region. */
        private final class ScopedOutput implements JavaLexicalOutput {
            private final int start;
            private final int end;
            private final Thread owner = Thread.currentThread();
            private OutputState state = OutputState.OPEN;
            private int emittedEnd;

            private ScopedOutput(int start, int end) {
                this.start = start;
                this.end = end;
            }

            @Override
            public void token(Token.Type type, String value, int from, int to) {
                ordered(from, to);
                if (Objects.requireNonNull(type, "type") == JavaToken.END_OF_FILE) {
                    throw new IllegalArgumentException("The tokenizer owns the final end-of-file token");
                }
                record(type, Objects.requireNonNull(value, "value"), translation.originalOffset(start + from),
                        translation.originalOffset(start + to));
            }

            @Override
            public void comment(JavaComment.Kind kind, int from, int to) {
                ordered(from, to);
                addComment(Objects.requireNonNull(kind, "kind"), start + from, start + to);
            }

            @Override
            public void problem(ProblemKind kind, String message, int from, int to) {
                range(from, to);
                report(kind, message, start + from, start + to);
            }

            private void ordered(int from, int to) {
                range(from, to);
                if (from == to || from < emittedEnd) {
                    throw new IllegalArgumentException("Lexical emissions must have positive, ordered, disjoint ranges");
                }
                emittedEnd = to;
            }

            private void range(int from, int to) {
                if (state != OutputState.OPEN || Thread.currentThread() != owner) {
                    throw new IllegalStateException("Lexical emission is confined to its recognition callback");
                }
                Objects.checkFromToIndex(from, to, end - start);
            }
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
