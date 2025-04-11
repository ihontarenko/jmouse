package org.jmouse.el.renderable;

import org.jmouse.core.convert.Conversion;
import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.core.reflection.TypeInformation;
import org.jmouse.el.StringSource;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.ScopedChain;
import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.lexer.TokenizableSource;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.BinaryOperation;
import org.jmouse.el.node.expression.FunctionNode;
import org.jmouse.el.node.expression.LiteralNode;
import org.jmouse.el.renderable.evaluation.LoopVariables;
import org.jmouse.el.renderable.node.*;
import org.jmouse.el.renderable.node.sub.ConditionBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.lang.String.valueOf;

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

        if (expression instanceof FunctionNode) {
            expression.accept(this);
        } else {
            Conversion conversion = context.getConversion();
            Object     evaluated  = expression.evaluate(context);
            if (evaluated != null) {
                content.append(conversion.convert(evaluated, String.class));
            }
        }
    }

    @Override
    public void visit(BlockNode node) {
        if (node.getName().evaluate(context) instanceof String name) {
            Block block = registry.getBlock(name);
            if (block != null && block.node() instanceof BlockNode actual) {
                actual.getBody().accept(this);
                LOGGER.info("Block '{}' rendered", name);
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
            TokenizableSource source = new StringSource("fake: " + path, "");
            Template          real   = engine.getTemplate(path);
            Template          fake   = engine.newTemplate(embedNode.getBody(), source);
            EvaluationContext ctx    = fake.newContext();

            // Establish inheritance: set the parent of the fake template to be the real template.
            fake.setParent(real, ctx);

            // Render the fake template using a new renderer instance and the fresh context.
            Content inner = new TemplateRenderer(engine).render(fake, ctx);

            LOGGER.info("Embed '{}' rendered", path);

            // Append the rendered content of the embedded template to the current content.
            content.append(inner);
        }
    }

    /**
     * Processes an {@link IfNode} by iterating over its condition branches.
     * <p>
     * For each {@link ConditionBranch} in the IfNode, the "when" expression is evaluated. If the "when"
     * expression is {@code null}, this branch is considered the 'else' clause. Otherwise, the expression
     * is evaluated and converted to a Boolean. When a branch evaluates to {@code true} and has a corresponding
     * "then" node, that node is processed and further branch evaluation is terminated.
     * </p>
     *
     * @param ifNode the if-statement node to be processed
     */
    @Override
    public void visit(IfNode ifNode) {
        Conversion conversion = context.getConversion();

        for (ConditionBranch branch : ifNode.getBranches()) {
            // If "when" is null, the branch represents the "else" clause.
            ExpressionNode when      = branch.getWhen();
            Node           then      = branch.getThen();
            boolean        satisfied = when == null;

            if (when != null) {
                // Evaluate the condition and convert it to a Boolean.
                satisfied = conversion.convert(when.evaluate(context), Boolean.class);
            }

            // Process the "then" block if the condition is satisfied and a branch exists.
            if (satisfied && then != null) {
                then.accept(this);
                break;
            }
        }
    }

    /**
     * Processes a ForNode by evaluating its iterable expression and executing the loop body for each element.
     * If the iterable is empty and an "empty" block is defined, that block is executed instead.
     *
     * @param forNode the for-loop node containing the loop variable, iterable expression, loop body,
     *                and optionally an empty block for when the iterable is empty
     */
    @Override
    public void visit(ForNode forNode) {
        // Evaluate the iterable expression.
        Object             evaluated = forNode.getIterable().evaluate(context);
        ClassTypeInspector type      = TypeInformation.forInstance(evaluated);
        Iterable<?>        iterable  = null;
        Node               empty     = forNode.getEmpty();

        // Convert evaluated object to an Iterable if possible.
        if (type.isIterable()) {
            iterable = ((Iterable<?>) evaluated);
        } else if (type.isMap()) {
            iterable = ((Map<?, ?>) evaluated).entrySet();
        } else if (type.isString()) {
            // Splits the string into a list of single-character strings
            iterable = ((String) evaluated).chars().mapToObj(c -> valueOf((char) c)).toList();
        } else if (type.isArray()) {
            iterable = List.of(((Object[]) evaluated));
        }

        if (iterable != null && iterable.iterator().hasNext()) {
            Iterator<?>   iterator = iterable.iterator();
            ScopedChain   scope    = context.getScopedChain();
            int           counter  = 0;
            Node          body     = forNode.getBody();
            String        name     = forNode.getItem();
            LoopVariables loop     = new LoopVariables();

            // Iterate over each element, updating loop variables and scope.
            while (iterator.hasNext()) {
                scope.push();

                Object item = iterator.next();

                loop.setValue(item);
                loop.setLast(!iterator.hasNext());
                loop.setFirst(counter == 0);
                loop.setIndex(counter++);

                scope.setValue("loop", loop);

                // If the current loop item is a Map.Entry, extract its value as the loop item
                // and register its key in the loop variables.
                if (item instanceof Map.Entry<?, ?> entry) {
                    item = entry.getValue();
                    loop.setKey(entry.getKey());
                }

                scope.setValue(name, item);

                body.accept(this);

                scope.pop();
            }
        } else if (empty != null) {
            // Process the empty block if the iterable has no elements.
            empty.accept(this);
        }
    }

    /**
     * Processes a FunctionNode by determining whether it should be evaluated as a function or as a macro.
     * <p>
     * The method first checks the extension container for a function definition with the given name.
     * If no function is found, it looks up a macro by the same name in the template registry and, if present,
     * evaluates the macro. Otherwise, it directly evaluates the function node, converts the result to a String,
     * and appends it to the output content.
     * </p>
     *
     * @param node the FunctionNode representing a function or macro call to be processed
     */
    @Override
    public void visit(FunctionNode node) {
        ExtensionContainer extensions = context.getExtensions();
        String             name       = node.getName();
        Conversion         conversion = context.getConversion();

        // If no function is defined in extensions, attempt to process it as a macro.
        if (extensions.getFunction(name) == null) {
            Macro macro = registry.getMacro(name);
            if (macro != null) {
                macro.evaluate(this, node, context);
                LOGGER.info("Macro '{}' evaluated", name);
            }
        } else {
            // Evaluate the function node and convert the result to a String.
            Object evaluated = node.evaluate(context);
            content.append(conversion.convert(evaluated, String.class));
            LOGGER.info("Function '{}' evaluated", name);
        }
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
