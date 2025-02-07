package org.jmouse.expression.ast;

import org.jmouse.common.ast.node.EntryNode;

import java.util.ArrayList;
import java.util.List;

public class ParametersNode extends EntryNode {

    private final List<ParameterNode> parameters = new ArrayList<>();

    public List<ParameterNode> getParameters() {
        return parameters;
    }

    public void addParameter(ParameterNode parameter) {
        this.parameters.add(parameter);
    }

}
