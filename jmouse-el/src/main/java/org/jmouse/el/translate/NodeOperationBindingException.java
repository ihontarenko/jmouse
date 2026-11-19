package org.jmouse.el.translate;

/** An operation cannot silently skip a node without an exact handler. */
public final class NodeOperationBindingException extends TranslationRefusedException {
    private final String operation;
    private final String nodeKind;

    public NodeOperationBindingException(String operation, String nodeKind) {
        super("No handler for " + nodeKind + " in " + operation);
        this.operation = operation;
        this.nodeKind = nodeKind;
    }

    public String operation() {
        return operation;
    }

    public String nodeKind() {
        return nodeKind;
    }
}
