package org.jmouse.el.renderable;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.core.reflection.TypeInformation;
import org.jmouse.el.StringSource;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.ScopedChain;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.BinaryOperation;
import org.jmouse.el.node.expression.FunctionNode;
import org.jmouse.el.node.expression.FunctionNotFoundException;
import org.jmouse.el.node.expression.LiteralNode;
import org.jmouse.el.renderable.node.*;
import org.jmouse.el.renderable.node.sub.ConditionBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

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
//            evaluated = expression.evaluate(context);
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
        if (node.getName().evaluate(context) instanceof String name) {
            Block block = registry.getBlock(name);
            if (block != null && block.node() instanceof BlockNode actual) {
                LOGGER.info("Block '{}' will be rendered", name);
                actual.getBody().accept(this);
            }
        }
    }

    /**
     * Visits an IncludeNode.
     *
     * @param include the include node to process
     */
    @Override
    public void visit(IncludeNode include) {
        if (include.getPath().evaluate(context) instanceof String name) {
            Template          included = registry.getEngine().getTemplate(name);
            EvaluationContext ctx      = included.newContext();
            Node              root     = included.getRoot();

            LOGGER.info("Include '{}' template", name);

            root.accept(new PreProcessingVisitor(included, ctx));
            root.accept(new RenderVisitor(content, included.getRegistry(), ctx));
        }
    }


    /**
     * Processes an embed node by rendering an embedded template and appending its content.
     * <p>
     * The method works as follows:
     * <ol>
     *   <li>Evaluates the path expression of the embed node to obtain the template path.</li>
     *   <li>If the path is valid (i.e. a String), it retrieves the "real" template from the engine.</li>
     *   <li>It then creates a new "fake" template using the embed node's body and a generated source identifier.
     *       This fake template represents a temporary template that wraps the embed node's content.</li>
     *   <li>A new evaluation context is created for the fake template.</li>
     *   <li>The fake template's parent is set to the real template, establishing an inheritance relationship.</li>
     *   <li>The fake template is rendered via a new {@link TemplateRenderer}, and its content is appended to the current output.</li>
     * </ol>
     * If the path expression does not evaluate to a String, the embed node is not processed.
     * </p>
     *
     * @param embedNode the embed node containing the path expression and the body to be embedded
     */
    @Override
    public void visit(EmbedNode embedNode) {
        // Evaluate the embed node's path to obtain the template path.
        if (embedNode.getPath().evaluate(context) instanceof String path) {
            // Retrieve the engine from the template registry.
            // Create a dummy TokenizableSource; the source identifier includes the embedded template path.
            // Obtain the target (real) template by path from the engine.
            // Create a new "fake" template from the embed node's body and the dummy source.
            // Create a new evaluation context for the fake template.
            Engine            engine = registry.getEngine();
            TokenizableSource source = new StringSource("embedded: " + path, "");
            Template          real   = engine.getTemplate(path);
            Template          fake   = engine.newTemplate(embedNode.getBody(), source);
            EvaluationContext ctx    = fake.newContext();

            // Establish inheritance: set the parent of the fake template to be the real template.
            fake.setParent(real, ctx);

            // Render the fake template using a new renderer instance and the fresh context.
            Content inner = new TemplateRenderer(engine).render(fake, ctx);

            // Append the rendered content of the embedded template to the current content.
            content.append(inner);
        }
    }

    @Override
    public void visit(IfNode ifNode) {
        for (ConditionBranch branch : ifNode.getBranches()) {
            System.out.println(branch);
        }
    }

    @Override
    public void visit(ForNode forNode) {
        Object             evaluated = forNode.getIterable().evaluate(context);
        ClassTypeInspector type      = TypeInformation.forInstance(evaluated);
        Iterable<?>        iterable  = null;

        if (type.isIterable()) {
            iterable = ((Iterable<?>) evaluated);
        } else if (type.isMap()) {
            iterable = ((Map<?, ?>) evaluated).entrySet();
        } else if (type.isString()) {
            iterable = ((String) evaluated).chars().mapToObj(c -> String.valueOf((char) c)).toList();
        } else if (type.isArray()) {
            iterable = List.of(((Object[]) evaluated));
        }

        if ((iterable == null || !iterable.iterator().hasNext()) && forNode.getEmpty() != null) {
            forNode.getEmpty().accept(this);
        } else {
            Iterator<?> iterator = iterable.iterator();
            ScopedChain scope    = context.getScopedChain();
            int         counter  = 0;
            Node        body     = forNode.getBody();
            String      name     = forNode.getItem();

            while (iterator.hasNext()) {
                scope.push();

                Object        item = iterator.next();
                LoopVariables loop = new LoopVariables();

                loop.setValue(item);
                loop.setLast(!iterator.hasNext());
                loop.setFirst(counter == 0);
                loop.setIndex(counter++);

                scope.setValue("loop", loop);

                if (item instanceof Map.Entry<?,?> entry) {
                    item = entry.getValue();
                }

                scope.setValue(name, item);

                try {
                    body.accept(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                scope.pop();
            }
        }
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
