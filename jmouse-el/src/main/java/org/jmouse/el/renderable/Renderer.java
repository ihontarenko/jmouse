package org.jmouse.el.renderable;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Node;

/**
 * The Renderer interface defines the contract for rendering a template into a {@link Content} object.
 */
public interface Renderer {

    /**
     * Renders the given template using the specified evaluation context.
     *
     * @param template the template to render
     * @param context  the evaluation context containing variables and inheritance information
     * @return the rendered content as a {@link Content} object
     */
    Content render(Template template, EvaluationContext context);

    /**
     * Default implementation of the Renderer interface.
     * <p>
     * This implementation performs the following steps:
     * <ol>
     *     <li>Linking: it binds macros and blocks using {@code MacroLinker} and {@code BlockLinker}
     *         and performs pre-processing via {@code PreProcessingVisitor}.</li>
     *     <li>Merging: it merges the template registries from the inheritance stack to form an effective registry.</li>
     *     <li>Determining the root template: retrieves the uppermost template from the inheritance stack.</li>
     *     <li>Rendering: it invokes a {@code RenderVisitor} on the root node to produce the final content.</li>
     * </ol>
     * </p>
     */
    class Default implements Renderer {

        private final Engine engine;

        /**
         * Constructs a Default renderer with the specified engine.
         *
         * @param engine the template engine to use for loading templates and registries
         */
        public Default(Engine engine) {
            this.engine = engine;
        }

        /**
         * Renders the specified template into a Content object.
         *
         * @param template the template to render
         * @param context  the evaluation context
         * @return the rendered content
         */
        @Override
        public Content render(Template template, EvaluationContext context) {
            // Linking: process macros, blocks and other pre-processing steps.
            linking(template, context);

            // Merge template registries from the inheritance stack.
            TemplateRegistry registry = merge(context);
            // Determine the root (uppermost) template from the inheritance stack.
            Template root = getRootTemplate(context);
            // Get the root node of the template.
            Node node = root.getRoot();

            // Create an initial Content object.
            Content content = Content.array();

            // Render the node using RenderVisitor.
            node.accept(new RenderVisitor(content, registry, context));

            return content;
        }

        /**
         * Retrieves the root template from the evaluation context's inheritance stack.
         *
         * @param context the evaluation context containing the inheritance stack
         * @return the uppermost template in the inheritance stack
         */
        private Template getRootTemplate(EvaluationContext context) {
            return context.getInheritance().getUpper();
        }

        /**
         * Merges the template registries from the entire inheritance stack.
         *
         * @param context the evaluation context containing the inheritance stack
         * @return the merged TemplateRegistry
         */
        private TemplateRegistry merge(EvaluationContext context) {
            Inheritance      inheritance = context.getInheritance();
            TemplateRegistry registry    = new TemplateRegistry(engine);

            for (Template template : inheritance.getStack()) {
                registry = registry.merge(template.getRegistry());
            }

            return registry;
        }

        /**
         * Performs linking of the template.
         * <p>
         * This method recursively applies MacroLinker, BlockLinker, and PreProcessingVisitor to
         * the template and its parent templates.
         * </p>
         *
         * @param template the template to link
         * @param context  the evaluation context
         */
        private void linking(Template template, EvaluationContext context) {
            Node root = template.getRoot();

            // Link macros, blocks and perform pre-processing.
            root.accept(new MacroLinker(template));
            root.accept(new BlockLinker(template, context));
            root.accept(new PreProcessingVisitor(template, context));

            Template parent = template.getParent(context);

            if (parent != null) {
                context.getInheritance().ascend();
                linking(parent, context);
                context.getInheritance().descend();
            }
        }
    }
}
