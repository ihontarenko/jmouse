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
            // Фаза 1: Рекурсивно виконуємо препроцесінг для поточного шаблону та його батьків
            preprocessTemplate(template, evaluationContext);

            // Фаза 2: Рекурсивно зливаємо визначення з усіх шаблонів
            TemplateRegistry effectiveRegistry = mergeRegistries(template);

            // Фаза 3: Знаходимо базовий шаблон (той, що не має батьків)
            Template baseTemplate = getRootTemplate(template);
            // За потреби можна додати інші візітори

            // jMEL

            // Фаза 5: Рендеримо базовий шаблон за допомогою CompositeVisitor
            Content content = new Content();
            baseTemplate.getRoot().accept(compositeVisitor);

            return finalContent;
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

    }

}
