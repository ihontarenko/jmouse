package org.jmouse.template;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Node;
import org.jmouse.el.rendering.Template;
import org.jmouse.template.node.MacroNode;

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
