package org.jmouse.expression.ast;

import org.jmouse.common.ast.node.EntryNode;
import org.jmouse.common.ast.node.Node;

public class StringDefinitionNode extends EntryNode {

    private Node handler;
    private Node command;

    public Node getHandler() {
        return handler;
    }

    public void setHandler(Node handler) {
        this.handler = handler;
    }

    public Node getCommand() {
        return command;
    }

    public void setCommand(Node command) {
        this.command = command;
    }

}
