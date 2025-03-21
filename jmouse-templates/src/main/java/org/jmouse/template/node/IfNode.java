package org.jmouse.template.node;

import org.jmouse.el.node.AbstractRenderableNode;

import java.util.ArrayList;
import java.util.List;

public class IfNode extends AbstractRenderableNode {

    private final List<IfCondition> conditions;

    public IfNode() {
        this.conditions = new ArrayList<>();
    }

    public void addCondition(IfCondition condition) {
        conditions.add(condition);
    }

    public void addElse(IfCondition condition) {
        conditions.addLast(condition);
    }

    public List<IfCondition> getConditions() {
        return conditions;
    }

}
