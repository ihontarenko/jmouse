package org.jmouse.el.renderable;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.core.evaluation.EvaluationContext;
import org.jmouse.el.core.node.Node;
import org.jmouse.el.core.rendering.Content;
import org.jmouse.el.core.rendering.Template;
import org.jmouse.el.renderable.node.BlockNode;
import org.jmouse.el.renderable.node.IncludeNode;
import org.jmouse.el.renderable.node.RawTextNode;

public class RenderVisitor implements NodeVisitor {

    private final EvaluationContext context;
    private final TemplateRegistry  registry;
    private final Content  content;
    private final Template template;

    public RenderVisitor(TemplateRegistry registry, EvaluationContext context, Content content, Template template) {
        this.context = context;
        this.registry = registry;
        this.content = content;
        this.template = template;
    }

    @Override
    public void visit(RawTextNode node) {
        content.append(node.getString());
    }

    @Override
    public void visit(BlockNode node) {
        Conversion conversion = context.getConversion();
        Object evaluated = node.getName().evaluate(context);
        Node actual = template.getBlock(conversion.convert(evaluated, String.class)).getBlockNode();
    }

    @Override
    public void visit(IncludeNode includeNode) {
        Template         includedTemplate = registry.getEngine().getTemplate(includeNode.getIncludedTemplateName());
        TemplateRenderer templateRenderer = new Renderer.Default(registry.getEngine());
        Content          includedContent  = templateRenderer.render(includedTemplate, context);
        content.append(includedContent.toString());
    }
    // Для ExtendNode та ImportNode рендеринг не генерує вивід
}
