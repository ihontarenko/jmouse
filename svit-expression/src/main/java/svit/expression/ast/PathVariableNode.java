package svit.expression.ast;

import svit.ast.node.EntryNode;

import java.util.Objects;

public class PathVariableNode extends EntryNode {

    private String name;

    public PathVariableNode(String name) {
        this.name = Objects.requireNonNull(name).substring(1);
        setAttribute("variable", this.name);
    }

    public String getName() {
        return name;
    }

}
