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
import org.jmouse.el.node.expression.FilterNode;
import org.jmouse.el.node.expression.FunctionNode;
import org.jmouse.el.node.expression.MapNode;
import org.jmouse.el.node.expression.literal.StringLiteralNode;
import org.jmouse.el.renderable.evaluation.LoopVariables;
import org.jmouse.el.renderable.node.*;
import org.jmouse.el.renderable.node.sub.ConditionBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.lang.String.valueOf;

/**
 * The RendererVisitor traverses the template AST and produces the final rendered output.
 * <p>
 * It processes various node types such as container, text, print, block, include, embed,
 * if, for, function, and apply nodes. The visitor uses the provided evaluation context,
 * template registry, and content accumulator to render the template.
 * </p>
 */
public class RendererVisitor implements NodeVisitor {

    private final static Logger LOGGER = LoggerFactory.getLogger(RendererVisitor.class);

    private final EvaluationContext context;
    private final TemplateRegistry  registry;
    private final Content           content;

    /**
     * Constructs a RendererVisitor with the specified content accumulator, template registry, and evaluation context.
     *
     * @param content  the content accumulator for the rendered output
     * @param registry the template registry containing block and macro definitions
     * @param context  the evaluation context used for expression evaluation and variable scope resolution
     */
    public RendererVisitor(Content content, TemplateRegistry registry, EvaluationContext context) {
        this.context = context;
        this.registry = registry;
        this.content = content;
    }

    /**
     * Processes a ContainerNode by recursively visiting its children.
     *
     * @param container the container node holding child nodes
     */
    @Override
    public void visit(ContainerNode container) {
        for (Node child : container.getChildren()) {
            child.accept(this);
        }
    }

    /**
     * Processes a TextNode by appending its text content to the output.
     *
     * @param node the text node whose content is appended
     */
    @Override
    public void visit(TextNode node) {
        content.append(node.getString());
    }

    /**
     * Processes a PrintNode by evaluating its expression and appending the result to the output.
     * <p>
     * If the expression is an instance of FunctionNode, it delegates to that node's visitor method.
     * Otherwise, the expression is evaluated, converted to a String, and appended to the output.
     * </p>
     *
     * @param printNode the print node to process
     */
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

    /**
     * Visits a DoNode.
     *
     * @param doNode the set node to process
     */
    @Override
    public void visit(DoNode doNode) {
        ExpressionNode expression = doNode.getExpression();
        Object         evaluated  = expression.evaluate(context);
        LOGGER.info("Do: {}; Result: {}", expression, evaluated);
    }

    /**
     * Processes a ScopeNode AST node
     *
     * @param scopeNode the set node to process
     */
    @Override
    @SuppressWarnings("unchecked")
    public void visit(ScopeNode scopeNode) {
        Node        body  = scopeNode.getBody();
        MapNode     with  = scopeNode.getWith();
        ScopedChain chain = context.getScopedChain();

        chain.push();

        if (with != null && with.evaluate(context) instanceof Map<?, ?> map) {
            ((Map<String, Object>) map).forEach(context::setValue);
        }

        body.accept(this);

        chain.pop();
    }

    /**
     * Visits a SetNode.
     *
     * @param setNode the block node to process
     */
    @Override
    public void visit(SetNode setNode) {
        context.setValue(setNode.getVariable(), setNode.getValue().evaluate(context));
    }

    /**
     * Visits a RenderNode.
     *
     * @param renderNode the set node to process
     */
    @Override
    public void visit(RenderNode renderNode) {
        if (renderNode.getName().evaluate(context) instanceof String string) {
            registry.getBlock(string).node().accept(this);
        }
    }

    /**
     * Processes a BlockNode by finding the corresponding block in the template registry and rendering its body.
     *
     * @param node the block node to process
     */
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
     * Processes an IncludeNode by loading an external template and rendering it within the current output.
     *
     * @param include the include node specifying the external template to include
     */
    @Override
    public void visit(IncludeNode include) {
        if (include.getPath().evaluate(context) instanceof String name) {
            Template          included = registry.getEngine().getTemplate(name);
            EvaluationContext ctx      = included.newContext();
            Node              root     = included.getRoot();

            LOGGER.info("Include '{}' template", name);

            root.accept(new InitializerVisitor(included, ctx));
            root.accept(new RendererVisitor(content, included.getRegistry(), ctx));
        }
    }


    /**
     * Processes an EmbedNode by rendering an embedded template and appending its output.
     * <p>
     * The embed node's path expression is evaluated to obtain the embedded template path.
     * A temporary (fake) template is created using the embed node's body, and its parent is set
     * to the real template loaded via the engine. A new evaluation context is created for the fake template.
     * Finally, the fake template is rendered and its content appended to the output.
     * </p>
     *
     * @param embedNode the embed node to process
     */
    @Override
    public void visit(EmbedNode embedNode) {
        // Evaluate the embed node's path to obtain the template path.
        if (embedNode.getPath().evaluate(context) instanceof String path) {
            // 1. Retrieve the engine from the template registry.
            // 2. Create a dummy TokenizableSource; the source identifier includes the embedded template path.
            // 3. Obtain the target (real) template by path from the engine.
            // 4. Create a new "fake" template from the embed node's body and the dummy source.
            // 5. Create a new evaluation context for the fake template.
            Engine            engine          = registry.getEngine();
            TokenizableSource source          = new StringSource("embedded: " + path, "");
            Template          real            = engine.getTemplate(path);
            Template          fake            = engine.newTemplate(embedNode.getBody(), source);
            EvaluationContext embeddedContext = fake.newContext();

            // 1. Link scoped values to embedded template
            // 2. Import registry from root to fake
            // 3. Establish inheritance: set the parent of the fake template to be the real template.
            embeddedContext.setScopedChain(context.getScopedChain());
            fake.getRegistry().copyFrom(registry);
            fake.setParent(real, embeddedContext);

            // Render the fake template using a new renderer instance and the fresh context.
            Content inner = new TemplateRenderer(engine).render(fake, embeddedContext);

            LOGGER.info("Embed '{}' rendered", path);

            // Append the rendered content of the embedded template to the current content.
            content.append(inner);
        }
    }

    /**
     * Processes an IfNode by evaluating each condition branch sequentially.
     * <p>
     * For each branch, if the "when" expression is null (representing the else branch) or evaluates
     * to true (after conversion to Boolean), the corresponding "then" block is processed and further
     * evaluation is stopped.
     * </p>
     *
     * @param ifNode the if node to process
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

                loop.setFirst(counter == 0);
                loop.setIndex(counter++);
                loop.setKey(counter);
                loop.setValue(item);
                loop.setLast(!iterator.hasNext());

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
     * Processes a FunctionNode by checking for an extension-defined function.
     * <p>
     * If the extension container does not define a function with the given name,
     * the method attempts to evaluate it as a macro. Otherwise, it evaluates the function,
     * converts the result to a String, appends it to the output, and logs the event.
     * </p>
     *
     * @param node the function node to process
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

    /**
     * Processes an ApplyNode by rendering its body and applying a chain of filters.
     * <p>
     * The body of the ApplyNode is first rendered to produce an intermediate result.
     * Each filter in the chain is then applied sequentially to the result.
     * The final processed output is appended to the current content.
     * </p>
     *
     * @param applyNode the apply node containing a body and a chain of filters to apply
     */
    @Override
    public void visit(ApplyNode applyNode) {
        // Render the body of the apply node.
        Content temporary = Content.array();
        Node    body      = applyNode.getBody();

        body.accept(new RendererVisitor(temporary, registry, context));

        String inner = temporary.toString();

        // Apply each filter in the chain sequentially.
        for (FilterNode filter : applyNode.getChain()) {
            filter.setLeft(new StringLiteralNode(inner));
            if (filter.evaluate(context) instanceof String string) {
                inner = string;
            }
        }

        // Append the final filtered result to the overall content.
        content.append(inner);
    }
}
