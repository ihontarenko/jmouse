package org.jmouse.java.parser;

import java.util.List;
import org.jmouse.el.node.Trivia;
import org.jmouse.java.parser.grammar.unit.JavaUnitRoles;
import org.jmouse.java.parser.node.declaration.JavaClassDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaFieldDeclarationNode;
import org.jmouse.java.parser.node.unit.JavaCompilationUnitNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaSyntaxOriginTest {
    @Test
    void sourceSnapshotAndLeadingDeclarationCommentsSurviveCallerChanges() {
        String original = "// type\n/** Type documentation. */\nclass Example { /* field */ int value; }";
        StringBuilder mutable = new StringBuilder(original);
        JavaSyntaxParser parser = new JavaSyntaxParser();
        JavaSyntaxResult<JavaCompilationUnitNode> result = parser.parseSource(JavaUnitRoles.COMPILATION_UNIT, "Example.java", mutable);
        mutable.setLength(0);
        assertEquals(original, result.source().text());
        assertEquals(3, result.source().comments().size());
        assertThrows(UnsupportedOperationException.class, () -> result.source().tokens().clear());
        JavaClassDeclarationNode type = (JavaClassDeclarationNode) result.root().declarations().getFirst();
        assertEquals(List.of(Trivia.comment("// type"), Trivia.comment("/** Type documentation. */")),
                type.getLeadingTrivia());
        JavaFieldDeclarationNode field = (JavaFieldDeclarationNode) type.body().declarations().getFirst();
        assertEquals(List.of(Trivia.comment("/* field */")), field.getLeadingTrivia());
    }
}
