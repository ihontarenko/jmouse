package org.jmouse.el.renderable;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.BinaryOperation;
import org.jmouse.el.node.expression.FunctionNode;
import org.jmouse.el.node.expression.FunctionNotFoundException;
import org.jmouse.el.node.expression.LiteralNode;
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
    public void visit(ContainerNode container) {
        for (Node child : container.getChildren()) {
            child.accept(this);
        }
    }

    @Override
    public void visit(TextNode node) {
        content.append(node.getString());
    }

    @Override
    public void visit(PrintNode printNode) {
        ExpressionNode expression = printNode.getExpression();
        Object         evaluated  = null;
//
//        if (expression instanceof FunctionNode) {
//            expression.accept(this);
//        } else {
////            evaluated = expression.evaluate(context);
//        }

        try {
            expression.accept(this);
            evaluated = expression.evaluate(context);
        } catch (FunctionNotFoundException exception) {
            if (expression instanceof FunctionNode functionNode) {
                Macro macro = registry.getMacro(functionNode.getName());
                if (macro != null) {
                    macro.evaluate(this, functionNode, context);
                }
            }
        }

        if (evaluated != null) {
            content.append(context.getConversion().convert(evaluated, String.class));
        }
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

    /**
     * Visits an IncludeNode.
     *
     * @param include the include node to process
     */
    @Override
    public void visit(IncludeNode include) {
        Conversion conversion = context.getConversion();
        Object     path       = include.getPath().evaluate(context);
        String     name       = conversion.convert(path, String.class);
        Template   included   = registry.getEngine().getTemplate(name);

        EvaluationContext ctx  = included.newContext();
        Node              root = included.getRoot();

        ctx.setValue("parent", ctx.getInheritance().getCurrent());

        root.accept(new PreProcessingVisitor(included, ctx));
        root.accept(new RenderVisitor(content, included.getRegistry(), ctx));
    }

    @Override
    public void visit(IfNode ifNode) {
        System.out.println(ifNode);
    }

    @Override
    public void visit(FunctionNode function) {
        System.out.println("Function: " + function.getName());
        System.out.println("Is Macro: " + (context.getExtensions().getFunction(function.getName()) == null));
    }

    @Override
    public void visit(LiteralNode<?> literal) {
        System.out.println("Literal: " + literal.getValue());
    }

    @Override
    public void visit(BinaryOperation binary) {
        System.out.println("Binary: " + binary.toString());
    }

}
