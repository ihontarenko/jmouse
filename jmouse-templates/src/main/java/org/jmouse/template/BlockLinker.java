package org.jmouse.template;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Node;
import org.jmouse.el.rendering.Template;
import org.jmouse.template.node.BlockNode;

import java.util.function.Consumer;

public class BlockLinker implements Consumer<Node> {

    private final Template          template;
    private final EvaluationContext context;

    public BlockLinker(Template template, EvaluationContext context) {
        this.template = template;
        this.context = context;
    }

    @Override
    public void accept(Node node) {
        if (node instanceof BlockNode block) {
            Conversion conversion = context.getConversion();
            Object     name       = block.getName().evaluate(context);
            template.setBlock(new TemplateBlock(conversion.convert(name, String.class), block));
        }
    }

}
