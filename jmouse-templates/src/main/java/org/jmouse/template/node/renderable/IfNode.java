package org.jmouse.template.node.renderable;

import org.jmouse.template.node.AbstractRenderableNode;

import java.util.ArrayList;
import java.util.List;

public class IfNode extends AbstractRenderableNode {

    private final List<IfCondition>           conditions;

    public IfNode() {
        this.conditions = new ArrayList<>();
    }

    public void addCondition(IfCondition condition) {
        conditions.add(condition);
    }

    public void addElse(IfCondition condition) {
        conditions.addLast(condition);
    }

}
