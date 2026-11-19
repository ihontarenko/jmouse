package org.jmouse.java.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jmouse.java.parser.grammar.unit.JavaUnitRoles;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.projection.JavaDeclarationProjection;
import org.junit.jupiter.api.Test;

class JavaDeclarationProjectionTest {
    @Test
    void existingDeclarationRecordsAreProjectedFromTheFullTreeWithoutReparsing() {
        String source =
                """
                package p;
                import java.util.List;
                /** A box. */
                public class Box<T extends Number> {
                    /** Values. */
                    private final List<String> values = null;
                    /** Reads one value. */
                    public String get(int index) { return values.get(index); }
                    Box() {}
                }
                """;
        JavaSourceFile expected = new JavaParser().parse("Box.java", source);
        JavaSourceFile actual =
                JavaDeclarationProjection.project(
                        new JavaSyntaxParser()
                                .parseSource(JavaUnitRoles.COMPILATION_UNIT, "Box.java", source));
        assertEquals(expected.packageDeclaration(), actual.packageDeclaration());
        assertEquals(expected.imports(), actual.imports());
        assertEquals(expected.declarations(), actual.declarations());
        assertEquals(expected.javaDocs(), actual.javaDocs());
        assertEquals(expected.typeReferences(), actual.typeReferences());
    }
}
