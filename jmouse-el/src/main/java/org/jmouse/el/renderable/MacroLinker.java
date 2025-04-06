package org.jmouse.el.renderable;

import org.jmouse.el.core.evaluation.EvaluationContext;
import org.jmouse.el.core.node.Node;
import org.jmouse.el.core.rendering.Template;
import org.jmouse.el.renderable.node.MacroNode;

import java.util.function.Consumer;

public class MacroLinker implements Consumer<Node> {

    private final Template          template;
    private final EvaluationContext context;

    public MacroLinker(Template template, EvaluationContext context) {
        this.template = template;
        this.context = context;
    }

    @Override
    public void accept(Node node) {
        if (node instanceof MacroNode macroNode) {
            template.setMacro(new TemplateMacro(macroNode.getName()));
        }
    }

}
