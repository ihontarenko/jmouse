package org.jmouse.expression.ast;

import org.jmouse.common.ast.node.EntryNode;

public class VariableNode extends EntryNode {

    private String rawName;

    public String getRawName() {
        return rawName;
    }

    public String getVariableName() {
        return rawName.substring(1);
    }

    public VariableNode(String rawName) {
        this.rawName = rawName;
    }

}
