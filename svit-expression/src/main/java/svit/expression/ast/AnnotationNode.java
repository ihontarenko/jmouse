package svit.expression.ast;

import svit.ast.node.EntryNode;
import svit.ast.token.Token;

public class AnnotationNode extends EntryNode {
    public AnnotationNode(Token.Entry entry) {
        super(entry);
    }
}
