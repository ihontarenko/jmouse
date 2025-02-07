package org.jmouse.expression.ast;

import org.jmouse.common.ast.node.EntryNode;

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
