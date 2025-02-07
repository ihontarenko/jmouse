package org.jmouse.expression.ast;

import org.jmouse.common.ast.node.EntryNode;
import org.jmouse.common.ast.node.Node;

public class ObjectMethodNode extends EntryNode {

    private String objectName;
    private String methodName;
    private Node   arguments;

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Node getArguments() {
        return arguments;
    }

    public void setArguments(Node arguments) {
        this.arguments = arguments;
    }

}
