package org.jmouse.java.parser;

import java.util.ArrayList;
import java.util.List;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Trivia;
import org.jmouse.el.node.Visitor;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.node.CompilationUnitNode;
import org.jmouse.java.parser.node.ImportNode;
import org.jmouse.java.parser.node.JavaNodeVisitor;
import org.jmouse.java.parser.node.MemberNode;
import org.jmouse.java.parser.node.PackageNode;
import org.jmouse.java.parser.node.TypeNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The jME node tree: parentage, visitor traversal, comments as trivia, and the record projection.
 */
class NodeTreeTest {

    private static final String SOURCE = """
            package p;
            import java.util.List;
            // about the type
            /** The type. */
            class Outer {
                /* before the field */
                int field;
                /** The nested type. */
                static class Inner {
                    void method() { /** inside a body */ }
                }
            }
            """;

    private final CompilationUnitNode root = new JavaParser().parseTree("Outer.java", SOURCE);

    @Test
    void aJavaVisitorReceivesNodesByTypeInSourceOrder() {
        List<String> visited = new ArrayList<>();
        root.accept(new JavaNodeVisitor() {
            @Override
            public void visit(CompilationUnitNode compilationUnit) {
                visited.add("unit");
            }

            @Override
            public void visit(PackageNode packageNode) {
                visited.add("package " + packageNode.declaration().name());
            }

            @Override
            public void visit(ImportNode importNode) {
                visited.add("import " + importNode.declaration().name());
            }

            @Override
            public void visit(TypeNode typeNode) {
                visited.add("type " + typeNode.declaration().name());
            }

            @Override
            public void visit(MemberNode memberNode) {
                visited.add("member " + memberNode.declaration().name());
            }
        });

        assertEquals(List.of("unit", "package p", "import java.util.List", "type Outer", "member field", "type Inner", "member method"),
                visited);
    }

    @Test
    void aPlainJmouseElVisitorStillWalksTheTree() {
        List<Node> visited = new ArrayList<>();
        root.accept(new Visitor() {
            @Override
            public void visit(Node node) {
                visited.add(node);
            }
        });

        assertEquals(7, visited.size());
        assertSame(root, visited.getFirst());
    }

    @Test
    void commentsBeforeADeclarationAreItsLeadingTrivia() {
        TypeNode outer = (TypeNode) root.getChildren().get(2);
        assertEquals(List.of(Trivia.comment("// about the type"), Trivia.comment("/** The type. */")), outer.getLeadingTrivia());

        MemberNode field = (MemberNode) outer.getChildren().getFirst();
        assertEquals(List.of(Trivia.comment("/* before the field */")), field.getLeadingTrivia());
        assertSame(outer, field.getParent());
        assertEquals("The type.", outer.declaration().javaDoc().description());
    }

    @Test
    void theRecordModelIsAProjectionOfTheTree() {
        JavaSourceFile file = root.toSourceFile();

        assertTrue(file.isComplete());
        assertEquals(List.of("p.Outer", "p.Outer#field", "p.Outer.Inner", "p.Outer.Inner#method()"),
                file.declarations().stream().map(declaration -> declaration.symbolId().value()).toList());
        assertEquals(file, new JavaParser().parse("Outer.java", SOURCE));
        assertEquals(2, file.javaDocs().size());
    }
}
