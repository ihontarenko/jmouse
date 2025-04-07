package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.NodeVisitor;
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

}
