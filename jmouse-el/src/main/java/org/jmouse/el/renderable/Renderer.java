package org.jmouse.el.renderable;

import org.jmouse.el.evaluation.EvaluationContext;

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

            TemplateRegistry registry = merge(template, context);
            Template         root     = getRootTemplate(template, context);

            Content content = Content.array();

            root.getRoot().accept(new RenderVisitor(content, registry, context, template));

            return content;
        }

        private Template getRootTemplate(Template template, EvaluationContext context) {
            Template root = template;

            while (root.getParent(context) != null) {
                root = root.getParent(context);
            }

            return root;
        }

        private TemplateRegistry merge(Template template, EvaluationContext context) {
            TemplateRegistry merged = template.getRegistry();

            if (template.getParent(context) != null) {
                TemplateRegistry parent = merge(template.getParent(context), context);
                merged = parent.merge(merged);
            }

            return merged;
        }

        private void linking(Template template, EvaluationContext context) {
            template.getRoot().accept(new MacroLinker(template, context));
            template.getRoot().accept(new BlockLinker(template, context));
//            template.getRoot().accept(new PreProcessingVisitor(template, context));

            if (template.getParent(context) != null) {
                linking(template.getParent(context), context);
            }
        }

    }

}
