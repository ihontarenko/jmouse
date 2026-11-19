package org.jmouse.java.parser;

import java.util.List;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.projection.JavaDeclarationProjection;
import org.jmouse.java.parser.node.unit.JavaCompilationUnitNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The same recovery contract as the compatibility facade, exercised through the new full AST. */
class JavaSyntaxRecoveryTest extends RecoveryTest {
    private final JavaSyntaxParser syntaxParser = new JavaSyntaxParser();

    @Override
    protected JavaSourceFile read(String name, String text) {
        return JavaDeclarationProjection.project(syntaxParser.recoverCompilationUnit(name, text));
    }

    @Test
    void malformedPackageAndImportHeadersDoNotEscapeRecovery() {
        JavaSourceFile file = read("Headers.java", """
                package 42;
                import java.util.;
                import java.util.List;
                class Kept {}
                """);

        assertEquals(List.of("Kept"), file.types().stream().map(type -> type.name()).toList());
        assertEquals(1, file.imports().size());
        assertEquals(2, file.problems().size());
    }

    @Test
    void aBrokenModuleDirectiveKeepsFollowingDirectives() {
        JavaSyntaxResult<JavaCompilationUnitNode> result = syntaxParser.recoverCompilationUnit("module-info.java", """
                module example {
                    requires ;
                    exports example.api;
                    uses example.Service;
                }
                """);

        assertEquals(2, result.root().module().orElseThrow().directives().size());
        assertEquals(1, result.problems().size());
    }
}
