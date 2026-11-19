package org.jmouse.java.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jmouse.el.node.Node;
import org.jmouse.el.node.NodeLifecycle;
import org.jmouse.el.node.Trivia;
import org.jmouse.el.node.Visitor;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.node.declaration.JavaFieldDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaMethodDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaTypeDeclarationNode;
import org.jmouse.java.parser.node.unit.JavaCompilationUnitNode;
import org.jmouse.java.parser.node.unit.JavaImportDeclarationNode;
import org.jmouse.java.parser.node.unit.JavaPackageDeclarationNode;
import org.jmouse.java.parser.projection.JavaDeclarationProjection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class NodeTreeTest {
    private static final String SOURCE =
            """
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
    private final JavaParser parser = new JavaParser();
    private final JavaCompilationUnitNode root = parser.parseTree("Outer.java", SOURCE);

    @Test
    void theCommonVisitorReceivesAllDeclarationFamiliesInSourceOrder() {
        List<String> visited = new ArrayList<>();
        root.accept(
                new Visitor() {
                    @Override
                    public void visit(Node node) {
                        if (node instanceof JavaCompilationUnitNode) {
                            visited.add("unit");
                        } else if (node instanceof JavaPackageDeclarationNode) {
                            visited.add("package p");
                        } else if (node instanceof JavaImportDeclarationNode) {
                            visited.add("import java.util.List");
                        } else if (node instanceof JavaTypeDeclarationNode type) {
                            visited.add("type " + type.name().name());
                        } else if (node instanceof JavaFieldDeclarationNode field) {
                            field.variables()
                                    .forEach(
                                            variable ->
                                                    visited.add(
                                                            "member " + variable.name().name()));
                        } else if (node instanceof JavaMethodDeclarationNode method) {
                            visited.add("member " + method.name().name());
                        }
                    }
                });
        assertEquals(
                List.of(
                        "unit",
                        "package p",
                        "import java.util.List",
                        "type Outer",
                        "member field",
                        "type Inner",
                        "member method"),
                visited);
    }

    @Test
    void theSharedTraversalPreservesParentageAcrossTheFullFrozenTree() {
        List<Node> visited = new ArrayList<>();
        root.accept(
                new Visitor() {
                    @Override
                    public void visit(Node node) {
                        visited.add(node);
                        node.getChildren().forEach(child -> assertSame(node, child.getParent()));
                    }
                });
        assertTrue(visited.size() > 7);
        assertSame(root, visited.getFirst());
        assertEquals(NodeLifecycle.FROZEN, root.lifecycle());
    }

    @Test
    void commentsBeforeADeclarationRemainItsLeadingTrivia() {
        JavaTypeDeclarationNode outer = (JavaTypeDeclarationNode) root.declarations().getFirst();
        assertEquals(
                List.of(Trivia.comment("// about the type"), Trivia.comment("/** The type. */")),
                outer.getLeadingTrivia());
        JavaFieldDeclarationNode field =
                (JavaFieldDeclarationNode) outer.body().declarations().getFirst();
        assertEquals(List.of(Trivia.comment("/* before the field */")), field.getLeadingTrivia());
        assertSame(outer.body(), field.getParent());
    }

    @Test
    void declarationRecordsAreAViewOfTheSameSyntaxGrammar() {
        JavaSourceFile file =
                JavaDeclarationProjection.project(parser.parseSyntax("Outer.java", SOURCE));
        assertTrue(file.isSyntaxComplete());
        assertEquals(
                List.of("p.Outer", "p.Outer#field", "p.Outer.Inner", "p.Outer.Inner#method()"),
                file.declarations().stream()
                        .map(declaration -> declaration.symbolId().value())
                        .toList());
        assertEquals(file.declarations(), parser.parse("Outer.java", SOURCE).declarations());
        assertEquals(2, file.javaDocs().size());
    }
}
