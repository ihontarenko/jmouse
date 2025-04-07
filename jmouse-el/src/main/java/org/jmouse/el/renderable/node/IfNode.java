package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.renderable.node.sub.ConditionBranch;

import java.util.ArrayList;
import java.util.List;

public class IfNode extends AbstractNode {

    private final List<ConditionBranch> conditions;

    public IfNode() {
        this.conditions = new ArrayList<>();
    }

    public void addBranch(ConditionBranch condition) {
        conditions.add(condition);
    }

    public List<ConditionBranch> getBranches() {
        return conditions;
    }

}
