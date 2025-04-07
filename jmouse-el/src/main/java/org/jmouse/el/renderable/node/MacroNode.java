package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.Node;

import java.util.List;

public class MacroNode extends AbstractNode {

    private String       name;
    private List<String> arguments;
    private Node         body;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    public Node getBody() {
        return body;
    }

    public void setBody(Node body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "MACRO: %s(%s)".formatted(name, arguments);
    }
}
