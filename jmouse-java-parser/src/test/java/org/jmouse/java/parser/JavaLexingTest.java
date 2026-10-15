package org.jmouse.java.parser;

import java.util.List;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.java.parser.lexer.JavaComment;
import org.jmouse.java.parser.lexer.JavaLexer;
import org.jmouse.java.parser.lexer.JavaSourceText;
import org.jmouse.java.parser.lexer.JavaTokenKind;
import org.jmouse.java.parser.model.FieldDeclaration;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.model.MethodDeclaration;
import org.jmouse.java.parser.source.ProblemKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lexing that would fool brace and parenthesis matching if it were done naively.
 */
class JavaLexingTest {

    private final JavaParser parser = new JavaParser();

    private JavaSourceFile parseComplete(String text) {
        JavaSourceFile file = parser.parse("Tricky.java", text);
        assertTrue(file.isComplete(), () -> "Unexpected problems: " + file.problems());
        return file;
    }

    private static List<String> memberNames(JavaSourceFile file) {
        return file.types().getFirst().members().stream().map(member -> member.name()).toList();
    }

    @Test
    void textBlocksContainingBracesAndQuotesDoNotUnbalanceBodies() {
        String quotes = "\"\"\"";
        String text = "class Blocks {\n"
                + "    String json = " + quotes + "\n"
                + "        { \"open\": \"{\", \"close\": \"}\", \"quote\": \\\"\"\" , \"two\": \"\" }\n"
                + "        ) ] }}}\n"
                + "        " + quotes + ";\n"
                + "    void after() { String inner = " + quotes + "\n"
                + "        } } }\n"
                + "        " + quotes + "; }\n"
                + "    int last;\n"
                + "}\n";
        JavaSourceFile file = parseComplete(text);

        assertEquals(List.of("json", "after", "last"), memberNames(file));
    }

    @Test
    void stringsAndCharactersWithEscapedDelimiters() {
        JavaSourceFile file = parseComplete("""
                class Strings {
                    String braces = "}}}{{{";
                    String escaped = "a\\"}\\\\";
                    char close = '}';
                    char quote = '\\'';
                    char backslash = '\\\\';
                    void method() { char open = '{'; String text = "{"; }
                    int last;
                }
                """);

        assertEquals(List.of("braces", "escaped", "close", "quote", "backslash", "method", "last"), memberNames(file));
    }

    @Test
    void commentsWithDelimiterLookalikesAreSkipped() {
        JavaSourceFile file = parseComplete("""
                class Comments {
                    // a line comment with } and /* and */ and "
                    /* a block comment with { and // and ** / and *\\/ */
                    /** documentation mentioning *&#47; and {@code }} and "quotes" */
                    int field;
                    /**/ int empty;
                    void method() { /* } */ // }
                    }
                    int last;
                }
                """);

        assertEquals(List.of("field", "empty", "method", "last"), memberNames(file));
        assertEquals("documentation mentioning *&#47; and {@code }} and \"quotes\"", file.types().getFirst().members().getFirst()
                .javaDoc().description());
        List<JavaComment.Kind> kinds = file.comments().stream().map(JavaComment::kind).toList();
        assertEquals(List.of(JavaComment.Kind.LINE, JavaComment.Kind.BLOCK, JavaComment.Kind.DOCUMENTATION, JavaComment.Kind.BLOCK,
                JavaComment.Kind.BLOCK, JavaComment.Kind.LINE), kinds);
    }

    @Test
    void nestedGenericsClosingWithShiftLookalikes() {
        JavaSourceFile file = parseComplete("""
                class Generics {
                    java.util.Map<String, java.util.Map<String, java.util.List<Integer>>> deep;
                    java.util.List<java.util.List<java.util.List<String>>> triple = new java.util.ArrayList<>();
                    int shifted = 8 >> 1, unsigned = -8 >>> 1, compared = 1 >= 0 ? 1 : 0;
                    <T extends Comparable<? super T>> java.util.List<java.util.List<T>> sort(java.util.Map<T, java.util.List<T>> input) { return null; }
                }
                """);

        assertEquals(List.of("deep", "triple", "shifted", "unsigned", "compared", "sort"), memberNames(file));
        assertEquals("java.util.Map<String, java.util.Map<String, java.util.List<Integer>>>",
                ((FieldDeclaration) file.types().getFirst().members().getFirst()).type());
        MethodDeclaration sort = (MethodDeclaration) file.types().getFirst().members().getLast();
        assertEquals("Generics#sort(java.util.Map)", sort.symbolId().value());
        assertEquals("java.util.List<java.util.List<T>>", sort.returnType());
    }

    @Test
    void unicodeEscapesAreTranslatedBeforeLexing() {
        // The escape for a double quote ends the string early only if the lexer forgets JLS 3.3.
        String text = "class Escapes {\n"
                + "    String text = \"a\\u0022;\n"
                + "    int \\u0061fter;\n"
                + "    String literal = \"\\\\u0022\";\n"
                + "}\n";
        JavaSourceFile file = parseComplete(text);

        assertEquals(List.of("text", "after", "literal"), memberNames(file));
        FieldDeclaration after = (FieldDeclaration) file.types().getFirst().members().get(1);
        assertEquals("\\u0061fter;", after.variableSpan().slice(text) + ";");
    }

    @Test
    void lexerSpeaksJmouseElTokenContracts() {
        TokenCursor cursor = new JavaLexer().tokenize(new JavaSourceText("Tokens.java", "a /* c */ >>= \"s\""));
        List<Token> tokens = cursor.toList();

        assertEquals(List.of(JavaTokenKind.IDENTIFIER, JavaTokenKind.GREATER, JavaTokenKind.GREATER, JavaTokenKind.ASSIGN,
                JavaTokenKind.STRING_LITERAL, JavaTokenKind.END_OF_FILE), tokens.stream().map(Token::type).toList());
        assertEquals(List.of(0, 1, 2, 3, 4, 5), tokens.stream().map(Token::ordinal).toList());
        JavaSourceText source = JavaLexer.sourceOf(cursor);
        assertEquals(1, source.comments().size());
        assertEquals(tokens.size(), source.size());
        assertEquals(17, source.endOffset(tokens.get(4)));
    }

    @Test
    void unterminatedLiteralsAreProblemsNotCrashes() {
        JavaSourceFile file = parser.parse("Broken.java", """
                class Broken {
                    String open = "never closed;
                    int swallowedByTheInitializer;
                    void after() { }
                }
                """);

        // The string closes at its line end; the initializer then runs to the next ';', so the damage
        // stays inside one statement and the method after it is intact.
        assertEquals(ProblemKind.UNTERMINATED_STRING, file.problems().getFirst().kind());
        assertEquals(2, file.problems().getFirst().span().startLine());
        assertEquals(List.of("open", "after"), memberNames(file));
    }

    @Test
    void unterminatedBlockCommentReachesTheEnd() {
        JavaSourceFile file = parser.parse("Comment.java", "class Comment { int kept; /* never closed\n int lost; }");

        assertTrue(file.problems().stream().anyMatch(problem -> problem.kind() == ProblemKind.UNTERMINATED_COMMENT));
        assertTrue(file.problems().stream().anyMatch(problem -> problem.kind() == ProblemKind.UNEXPECTED_END_OF_FILE));
        assertEquals(List.of("kept"), memberNames(file));
    }
}
