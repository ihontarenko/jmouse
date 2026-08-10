package org.jmouse.el.language.node;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.expression.ExpressionsNode;

import java.util.ArrayList;
import java.util.List;

public class IfNode extends ExpressionsNode {

    private final List<IfBranchNode> branches = new ArrayList<>();

    public void addBranch(IfBranchNode branch) {
        branches.add(branch);
    }

    public List<IfBranchNode> getBranches() {
        return List.copyOf(branches);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        for (IfBranchNode branch : branches) {
            if (branch.matches(context)) {
                return branch.evaluate(context);
            }
        }
        return null;
    }
}