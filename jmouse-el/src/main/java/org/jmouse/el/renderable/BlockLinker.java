package org.jmouse.el.renderable;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.core.evaluation.EvaluationContext;
import org.jmouse.el.core.node.NodeVisitor;
import org.jmouse.el.renderable.node.BlockNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Links BlockNode instances to the corresponding template.
 * <p>
 * The BlockLinker consumes Node objects, identifies BlockNodes, evaluates their names, converts
 * the names to String, and registers them with the template as TemplateBlocks.
 * </p>
 */
public class BlockLinker implements NodeVisitor {

    private final static Logger LOGGER = LoggerFactory.getLogger(BlockLinker.class);

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
    public void visit(BlockNode node) {
        Conversion       conversion = context.getConversion();
        Object           evaluated  = node.getName().evaluate(context);
        TemplateRegistry registry   = template.getRegistry();

        String name  = conversion.convert(evaluated, String.class);
        Block  block = new TemplateBlock(name, node);

        LOGGER.info("Registering block '{}' into '{}' template", name, template.getName());

        registry.registerBlock(name, block);
    }
}
