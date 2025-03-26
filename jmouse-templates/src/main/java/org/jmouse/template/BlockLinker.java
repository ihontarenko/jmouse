package org.jmouse.template;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Node;
import org.jmouse.el.rendering.RenderableEntity;
import org.jmouse.template.node.BlockNode;

import java.util.function.Consumer;

public class BlockLinker implements Consumer<Node> {

    private final RenderableEntity  entity;
    private final EvaluationContext context;

    public BlockLinker(RenderableEntity entity, EvaluationContext context) {
        this.entity = entity;
        this.context = context;
    }

    @Override
    public void accept(Node node) {
        if (node instanceof BlockNode block) {
            Conversion conversion = context.getConversion();
            Object name = block.getName().evaluate(context);
            entity.setBlock(new TemplateBlock(conversion.convert(name, String.class), block));
        }
    }

}
