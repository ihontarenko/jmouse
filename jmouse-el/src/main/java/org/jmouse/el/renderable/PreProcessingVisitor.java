package org.jmouse.el.renderable;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Node;
import org.jmouse.el.renderable.node.ContainerNode;
import org.jmouse.el.renderable.node.ExtendsNode;

public class PreProcessingVisitor implements NodeVisitor {

    private final EvaluationContext context;
    private final Template          template;

    public PreProcessingVisitor(Template template, EvaluationContext context) {
        this.context = context;
        this.template = template;
    }

    @Override
    public void visit(ContainerNode container) {
        for (Node child : container.getChildren()) {
            child.accept(this);
        }
    }

    @Override
    public void visit(ExtendsNode node) {
        Object     value      = node.getParent().evaluate(context);
        Conversion conversion = context.getConversion();
        String     location   = conversion.convert(value, String.class);
        Template   parent     = template.getRegistry().getEngine().getTemplate(location);

        template.setParent(parent, context);
    }


}