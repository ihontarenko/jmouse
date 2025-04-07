package org.jmouse.el.renderable;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.FunctionNode;
import org.jmouse.el.renderable.node.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenderVisitor implements NodeVisitor {

    private final static Logger LOGGER = LoggerFactory.getLogger(RenderVisitor.class);

    private final EvaluationContext context;
    private final TemplateRegistry  registry;
    private final Content           content;

    public RenderVisitor(Content content, TemplateRegistry registry, EvaluationContext context) {
        this.context = context;
        this.registry = registry;
        this.content = content;
    }

    @Override
    public void visit(TextNode node) {
        content.append(node.getString());
    }

    @Override
    public void visit(PrintNode printNode) {
        ExpressionNode expression = printNode.getExpression();
        Object         evaluated  = null;

        try {
            evaluated = expression.evaluate(context);
        } catch (Exception exception) {
            if (expression instanceof FunctionNode functionNode) {
                Macro macro = registry.getMacro(functionNode.getName());
                if (macro != null) {
                    macro.node().accept(this);
                }
            }
        }

        if (evaluated != null) {
            content.append(context.getConversion().convert(evaluated, String.class));
        }
    }

    @Override
    public void visit(MacroNode macroNode) {
        System.out.println(macroNode.getName());
    }

    @Override
    public void visit(BlockNode node) {
        Conversion conversion = context.getConversion();
        String     name       = conversion.convert(node.getName().evaluate(context), String.class);
        Block      block      = registry.getBlock(name);

        if (block != null && block.node() instanceof BlockNode actual) {
            LOGGER.info("Block '{}' will be rendered", name);
            actual.getBody().accept(this);
        }
    }

    @Override
    public void visit(IfNode ifNode) {
        System.out.println(ifNode);
    }

}
