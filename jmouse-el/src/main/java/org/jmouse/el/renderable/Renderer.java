package org.jmouse.el.renderable;

import org.jmouse.el.core.evaluation.EvaluationContext;

public interface Renderer {

    Content render(Template template, EvaluationContext context);

    class Default implements Renderer {

        private final Engine engine;

        public Default(Engine engine) {
            this.engine = engine;
        }

        @Override
        public Content render(Template template, EvaluationContext context) {
            linking(template, context);

            TemplateRegistry registry = mergeRegistries(template);
            Template root = getRootTemplate(template, context);

            Content content = Content.array();

            root.getRoot().accept(new RenderVisitor(registry, context, content, template));

            return content;
        }

        private Template getRootTemplate(Template template, EvaluationContext context) {
            Template root = template;

            while (root.getParent(context) != null) {
                root = root.getParent(context);
            }

            return root;
        }

        private TemplateRegistry mergeRegistries(Template template) {
            TemplateRegistry mergedRegistry = template.getRegistry();
            if (template.getParent() != null) {
                TemplateRegistry parentMerged = mergeRegistries(template.getParent());
                mergedRegistry = parentMerged.merge(mergedRegistry);
            }
            return mergedRegistry;
        }

        private void linking(Template template, EvaluationContext context) {
            template.getRoot().accept(new MacroLinker(template, context));
            template.getRoot().accept(new BlockLinker(template, context));

            if (template.getParent(context) != null) {
                linking(template.getParent(context), context);
            }
        }

    }

}
