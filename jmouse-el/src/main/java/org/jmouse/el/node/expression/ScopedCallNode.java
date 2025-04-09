package org.jmouse.el.node.expression;

public class ScopedCallNode extends FunctionNode {

    private String scope;

    /**
     * Constructs a FunctionNode with the specified function name.
     *
     * @param name the name of the function to be called
     */
    public ScopedCallNode(String name) {
        super(name);
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public String toString() {
        return "%s.%s".formatted(getScope(), super.toString());
    }
}
