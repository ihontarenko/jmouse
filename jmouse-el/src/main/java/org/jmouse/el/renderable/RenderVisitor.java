package org.jmouse.el.renderable;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Node;
import org.jmouse.el.renderable.node.BlockNode;
import org.jmouse.el.renderable.node.ContainerNode;
import org.jmouse.el.renderable.node.IfNode;
import org.jmouse.el.renderable.node.RawTextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenderVisitor implements NodeVisitor {

    private final static Logger LOGGER = LoggerFactory.getLogger(RenderVisitor.class);

    private final EvaluationContext context;
    private final TemplateRegistry  registry;
    private final Content           content;
    private final Template          template;

    public RenderVisitor(Content content, TemplateRegistry registry, EvaluationContext context, Template template) {
        this.context = context;
        this.registry = registry;
        this.content = content;
        this.template = template;
    }

    @Override
    public void visit(RawTextNode node) {
        content.append(node.getString());
    }

    @Override
    public void visit(BlockNode node) {
        Conversion conversion = context.getConversion();
        String     name       = conversion.convert(node.getName().evaluate(context), String.class);
        Block      block      = registry.getBlock(name);

        if (block != null && block.getBlockNode() instanceof BlockNode actual) {
            LOGGER.info("Block '{}' will be rendered", name);
            actual.accept(this);
        }
    }

    @Override
    public void visit(ContainerNode containerNode) {
        for (Node child : containerNode.children()) {
            child.accept(this);
        }
    }

    @Override
    public void visit(IfNode ifNode) {
        System.out.println(ifNode);
    }

}
