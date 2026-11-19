package org.jmouse.java.parser.projection;

import org.jmouse.el.node.Node;
import org.jmouse.el.node.Visitor;
import org.jmouse.java.parser.javadoc.JavaDocParser;
import org.jmouse.java.parser.lexer.JavaComment;
import org.jmouse.java.parser.lexer.JavaLexicalSnapshot;
import org.jmouse.java.parser.model.JavaDoc;
import org.jmouse.java.parser.model.JavaDocOwner;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.body.JavaAnonymousClassBodyNode;
import org.jmouse.java.parser.node.body.JavaConstructorBodySyntaxNode;
import org.jmouse.java.parser.node.statement.JavaBlockNode;
import org.jmouse.java.parser.source.SourceSpan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/** Owns documentation bindings and preserves the declaration view's treatment of body comments. */
final class JavaDocumentationProjection {
    private final JavaLexicalSnapshot source;
    private final NavigableMap<Integer, JavaComment> documentation = new TreeMap<>();
    private final Map<Integer, JavaDoc> parsed = new HashMap<>();
    private final Set<Integer> claimed = new HashSet<>();
    private final List<JavaDoc> owned = new ArrayList<>();
    private final List<SourceSpan> bodies = new ArrayList<>();
    private final JavaDocParser parser = new JavaDocParser();

    JavaDocumentationProjection(JavaLexicalSnapshot source, JavaSyntaxNode root) {
        this.source = source;
        for (JavaComment comment : source.comments()) {
            if (comment.kind() == JavaComment.Kind.DOCUMENTATION) {
                documentation.put(comment.span().start().offset(), comment);
            }
        }
        root.accept(
                new Visitor() {
                    @Override
                    public void visit(Node node) {
                        if (node instanceof JavaBlockNode
                                || node instanceof JavaConstructorBodySyntaxNode
                                || node instanceof JavaAnonymousClassBodyNode) {
                            bodies.add(((JavaSyntaxNode) node).span());
                        }
                    }
                });
    }

    JavaDoc owner(JavaSyntaxNode syntax, JavaDocOwner owner) {
        int start = syntax.span().start().offset();
        Map.Entry<Integer, JavaComment> candidate = documentation.floorEntry(start);
        if (candidate == null
                || candidate.getKey() < source.endBefore(start)
                || candidate.getValue().span().end().offset() > start) {
            return null;
        }
        claimed.add(candidate.getKey());
        JavaDoc result = parsed(candidate.getValue()).withOwner(owner);
        owned.add(result);
        return result;
    }

    List<JavaDoc> documents() {
        List<JavaDoc> documents = new ArrayList<>(owned);
        List<SourceSpan> sortedBodies =
                bodies.stream()
                        .sorted(Comparator.comparingInt(span -> span.start().offset()))
                        .toList();
        int bodyIndex = 0;
        int coveredUntil = -1;
        for (JavaComment comment : documentation.values()) {
            while (bodyIndex < sortedBodies.size()
                    && sortedBodies.get(bodyIndex).start().offset()
                            <= comment.span().start().offset()) {
                coveredUntil = Math.max(coveredUntil, sortedBodies.get(bodyIndex++).end().offset());
            }
            if (!claimed.contains(comment.span().start().offset())
                    && comment.span().end().offset() > coveredUntil) {
                documents.add(parsed(comment));
            }
        }
        documents.sort(Comparator.comparingInt(document -> document.span().start().offset()));
        return List.copyOf(documents);
    }

    private JavaDoc parsed(JavaComment comment) {
        return parsed.computeIfAbsent(
                comment.span().start().offset(),
                offset -> parser.parse(comment.text(), offset, source.lines(), null));
    }
}
