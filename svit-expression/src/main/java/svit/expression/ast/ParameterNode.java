package svit.expression.ast;

import svit.ast.node.EntryNode;
import svit.ast.node.Node;

public class ParameterNode extends EntryNode {

    private Node key;
    private Node value;

    public Node getKey() {
        return key;
    }

    public void setKey(Node key) {
        this.key = key;
    }

    public Node getValue() {
        return value;
    }

    public void setValue(Node value) {
        this.value = value;
    }

}
