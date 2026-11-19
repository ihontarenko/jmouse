package org.jmouse.java.parser.grammar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Trivia;
import org.jmouse.el.node.Visitor;
import org.jmouse.java.parser.lexer.JavaComment;
import org.jmouse.java.parser.lexer.JavaLexicalSnapshot;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.declaration.JavaDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaRecordComponentNode;
import org.jmouse.java.parser.node.module.JavaModuleDeclarationNode;
import org.jmouse.java.parser.node.parameter.JavaParameterNode;
import org.jmouse.java.parser.node.statement.JavaCatchClauseNode;
import org.jmouse.java.parser.node.statement.JavaFinallyClauseNode;
import org.jmouse.java.parser.node.statement.JavaStatementNode;
import org.jmouse.java.parser.node.unit.JavaPackageDeclarationNode;

/** Attaches leading comments through the common jME visitor; the lexical snapshot retains every comment. */
public final class JavaTriviaAttachment {
    private JavaTriviaAttachment() {
    }

    public static void attach(JavaSyntaxNode root, JavaLexicalSnapshot source) {
        if (source.comments().isEmpty()) {
            return;
        }
        List<JavaSyntaxNode> anchors = new ArrayList<>();
        root.accept(new Visitor() {
            @Override
            public void visit(Node node) {
                if (node instanceof JavaDeclarationNode || node instanceof JavaStatementNode
                        || node instanceof JavaParameterNode || node instanceof JavaRecordComponentNode
                        || node instanceof JavaPackageDeclarationNode || node instanceof JavaModuleDeclarationNode
                        || node instanceof JavaCatchClauseNode || node instanceof JavaFinallyClauseNode) {
                    anchors.add((JavaSyntaxNode) node);
                }
            }
        });
        if (anchors.isEmpty()) {
            anchors.add(root);
        }
        anchors.sort(Comparator.comparingInt(node -> node.span().start().offset()));
        int commentIndex = 0;
        for (JavaSyntaxNode anchor : anchors) {
            int start = anchor.span().start().offset();
            int previous = source.endBefore(start);
            List<Trivia> leading = new ArrayList<>();
            while (commentIndex < source.comments().size()
                    && source.comments().get(commentIndex).span().end().offset() <= start) {
                JavaComment comment = source.comments().get(commentIndex++);
                if (comment.span().start().offset() >= previous) {
                    leading.add(Trivia.comment(comment.text()));
                }
            }
            anchor.addLeadingTrivia(leading);
        }
    }
}
