package org.jmouse.el.renderable;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.core.evaluation.EvaluationContext;
import org.jmouse.el.core.node.Node;
import org.jmouse.el.renderable.node.BlockNode;

import java.util.function.Consumer;

/**
 * Links BlockNode instances to the corresponding template.
 * <p>
 * The BlockLinker consumes Node objects, identifies BlockNodes, evaluates their names, converts
 * the names to String, and registers them with the template as TemplateBlocks.
 * </p>
 */
public class BlockLinker implements Consumer<Node> {

    private final Template          template;
    private final EvaluationContext context;

    /**
     * Constructs a BlockLinker with the specified template and evaluation context.
     *
     * @param template the template to which blocks will be linked
     * @param context  the evaluation context used for evaluating and converting block names
     */
    public BlockLinker(Template template, EvaluationContext context) {
        this.template = template;
        this.context = context;
    }

    /**
     * Processes a node by linking BlockNode instances to the template.
     * <p>
     * If the node is a BlockNode, its name is evaluated and converted to a String,
     * then a new TemplateBlock is created and registered with the template.
     * </p>
     *
     * @param node the node to process
     */
    @Override
    public void accept(Node node) {
        if (node instanceof BlockNode block) {
            Conversion conversion = context.getConversion();
            Object     name       = block.getName().evaluate(context);
            template.setBlock(new TemplateBlock(conversion.convert(name, String.class), block));
        }
    }
}
