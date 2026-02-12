package org.jmouse.el.renderable;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of the Renderer interface.
 * <p>
 * This implementation performs the following steps:
 * <ol>
 *     <li>Linking: it binds macros and blocks using {@code MacroLinker} and {@code BlockLinker}
 *         and performs pre-processing via {@code InitializerVisitor}.</li>
 *     <li>Merging: it merges the view registries from the inheritance stack to form an effective registry.</li>
 *     <li>Determining the sourceRoot view: retrieves the uppermost view from the inheritance stack.</li>
 *     <li>Rendering: it invokes a {@code RendererVisitor} on the sourceRoot node to produce the final content.</li>
 * </ol>
 * </p>
 */
public class TemplateRenderer implements Renderer {

    private final static Logger LOGGER = LoggerFactory.getLogger(TemplateRenderer.class);
    private final        Engine engine;

    /**
     * Constructs a Default renderer with the specified engine.
     *
     * @param engine the view engine to use for loading templates and registries
     */
    public TemplateRenderer(Engine engine) {
        this.engine = engine;
    }

    /**
     * Renders the specified view into a Content object.
     *
     * @param template the view to render
     * @param context  the evaluation context
     * @return the rendered content
     */
    @Override
    public Content render(Template template, EvaluationContext context) {
        // Linking: process macros, blocks and other pre-processing steps.
        initialize(template, context);

        // 1. Merge view registries from the inheritance stack.
        // 2. Determine the sourceRoot (uppermost) view from the inheritance stack.
        // 3. Get the sourceRoot node of the view.
        TemplateRegistry registry = getRegistry(context);
        Template         root     = getRootTemplate(context);
        Node             node     = root.getRoot();

        LOGGER.info("Rendering view '{}' <- '{}'", root.getName(), template.getName());

        // Create an initial Content object.
        Content content = Content.array();

        // Render the node using RendererVisitor.
        node.accept(new RendererVisitor(content, registry, context));

        return content;
    }

    /**
     * Retrieves the sourceRoot view from the evaluation context's inheritance stack.
     *
     * @param context the evaluation context containing the inheritance stack
     * @return the uppermost view in the inheritance stack
     */
    private Template getRootTemplate(EvaluationContext context) {
        return context.getInheritance().getUpper();
    }

    /**
     * Merges the view registries from the entire inheritance stack.
     *
     * @param context the evaluation context containing the inheritance stack
     * @return the merged TemplateRegistry
     */
    private TemplateRegistry getRegistry(EvaluationContext context) {
        Inheritance      inheritance = context.getInheritance();
        TemplateRegistry registry    = new TemplateRegistry(engine);

        for (Template template : inheritance.getStack()) {
            registry = registry.merge(template.getRegistry());
        }

        return registry;
    }

    /**
     * Performs linking of the view.
     * <p>
     * This method recursively applies MacroLinker, BlockLinker, and InitializerVisitor to
     * the view and its parent templates.
     * </p>
     *
     * @param template the view to link
     * @param context  the evaluation context
     */
    private void initialize(Template template, EvaluationContext context) {
        Node root = template.getRoot();

        // Link macros, blocks and perform pre-processing.
        root.accept(new InitializerVisitor(template, context));

        Template parent = template.getParent(context);

        if (parent != null) {
            Inheritance inheritance = context.getInheritance();
            inheritance.ascend();
            initialize(parent, context);
            inheritance.descend();
        }
    }
}
