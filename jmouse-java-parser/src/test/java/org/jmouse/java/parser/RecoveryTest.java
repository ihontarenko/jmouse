package org.jmouse.java.parser;

import java.util.List;
import org.jmouse.java.parser.model.Declaration;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.model.TypeDeclaration;
import org.jmouse.java.parser.source.ProblemKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Malformed input yields positioned problems and keeps everything else.
 */
class RecoveryTest {

    private final JavaParser parser = new JavaParser();

    private static List<String> names(TypeDeclaration type) {
        return type.members().stream().map(Declaration::name).toList();
    }

    @Test
    void aBrokenMemberIsSkippedAndItsNeighboursKept() {
        JavaSourceFile file = parser.parse("Broken.java", """
                class Broken {
                    int before;
                    public void broken(int ) { return; }
                    private ??? garbage;
                    void after() {}
                    int last;
                }
                """);

        assertFalse(file.isComplete());
        assertEquals(List.of("before", "after", "last"), names(file.types().getFirst()));
        assertEquals(2, file.problems().size());
        assertEquals(3, file.problems().get(0).span().startLine());
        assertEquals(4, file.problems().get(1).span().startLine());
        assertTrue(file.problems().stream().allMatch(problem -> problem.kind() == ProblemKind.UNEXPECTED_TOKEN));
    }

    @Test
    void aBrokenTypeHeaderLosesOnlyThatType() {
        JavaSourceFile file = parser.parse("Two.java", """
                class 123Wrong { int lost; }
                class Right { int kept; }
                """);

        assertEquals(List.of("Right"), file.types().stream().map(TypeDeclaration::name).toList());
        assertEquals(1, file.problems().size());
        assertEquals(1, file.problems().getFirst().span().startLine());
    }

    @Test
    void anUnclosedBodyIsReportedAndKeptToTheEnd() {
        JavaSourceFile file = parser.parse("Open.java", """
                class Open {
                    int kept;
                    void open() {
                        if (true) {
                """);

        assertEquals(List.of("kept", "open"), names(file.types().getFirst()));
        assertTrue(file.problems().stream().anyMatch(problem -> problem.kind() == ProblemKind.UNBALANCED_DELIMITER));
        assertTrue(file.problems().stream().anyMatch(problem -> problem.kind() == ProblemKind.UNEXPECTED_END_OF_FILE));
    }

    @Test
    void aBrokenEnumConstantKeepsTheOthers() {
        JavaSourceFile file = parser.parse("Colours.java", """
                enum Colours {
                    RED, 42, BLUE;
                    int value;
                }
                """);

        assertEquals(List.of("RED", "BLUE", "value"), names(file.types().getFirst()));
        assertEquals(1, file.problems().size());
    }

    @Test
    void aMissingSemicolonIsAProblemNotACrash() {
        JavaSourceFile file = parser.parse("Missing.java", """
                class Missing {
                    int first
                    int second;
                    void method() {}
                }
                """);

        assertFalse(file.isComplete());
        assertTrue(names(file.types().getFirst()).contains("method"));
    }

    @Test
    void strayTopLevelTokensAndBracesAreSkipped() {
        JavaSourceFile file = parser.parse("Stray.java", """
                package p;
                }
                garbage here;
                class Kept {}
                """);

        assertEquals("p", file.packageName());
        assertEquals(List.of("Kept"), file.types().stream().map(TypeDeclaration::name).toList());
        assertEquals(2, file.problems().size());
    }

    @Test
    void anEmptyOrCommentOnlyFileIsComplete() {
        assertTrue(parser.parse("Empty.java", "").isComplete());
        assertTrue(parser.parse("Comments.java", "// only\n/* comments */").isComplete());
    }
}
