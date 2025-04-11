package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.NodeVisitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MacroNode extends AbstractNode {

    private String                      name;
    private List<String>                arguments;
    private Node                        body;
    private Map<String, ExpressionNode> defaultValues = new HashMap<>();

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

    public ExpressionNode getDefaultValue(String name) {
        return defaultValues.get(name);
    }

    public void setDefaultValues(Map<String, ExpressionNode> defaultValues) {
        this.defaultValues.putAll(defaultValues);
    }

    /**
     * Recursively executes the given consumer on this node and all its children.
     *
     * @param visitor the consumer to execute on each node
     */
    @Override
    public void accept(Visitor visitor) {
        if (visitor instanceof NodeVisitor nv) {
            nv.visit(this);
        }
    }

    @Override
    public String toString() {
        return "MACRO: %s(%s)".formatted(name, arguments);
    }
}
