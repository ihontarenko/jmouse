package org.jmouse.dom.template.transform;

import org.jmouse.core.Verify;
import org.jmouse.dom.template.NodeTemplate;
import org.jmouse.dom.template.RenderingExecution;
import org.jmouse.dom.template.TemplateRewrite;

import java.util.ArrayList;
import java.util.List;

public final class TemplateTraversal {

    public NodeTemplate traverse(
            NodeTemplate root,
            RenderingExecution execution,
            TemplateRewrite rewrite
    ) {
        Verify.nonNull(execution, "execution");
        Verify.nonNull(rewrite, "rewrite");
        return traverse(root, execution, rewrite, new TraversalContext());
    }

    public NodeTemplate traverse(
            NodeTemplate node,
            RenderingExecution execution,
            TemplateRewrite rewrite,
            TraversalContext context
    ) {
        if (node == null) {
            return null;
        }

        NodeTemplate transformed = switch (node) {
            case NodeTemplate.Element element -> {
                context.enter(element);
                List<NodeTemplate> children = mapList(element.children(), execution, rewrite, context);
                context.exit();

                yield new NodeTemplate.Element(
                        element.tagName(),
                        element.attributes(),
                        children,
                        element.directives()
                );
            }
            case NodeTemplate.Conditional conditional -> {
                List<NodeTemplate> branchA = mapList(conditional.whenTrue(), execution, rewrite, context);
                List<NodeTemplate> branchB = mapList(conditional.whenFalse(), execution, rewrite, context);
                yield new NodeTemplate.Conditional(conditional.predicate(), branchA, branchB);
            }
            case NodeTemplate.Repeat repeat -> {
                List<NodeTemplate> nodeTemplates = mapList(repeat.body(), execution, rewrite, context);
                yield new NodeTemplate.Repeat(repeat.collection(), repeat.itemVariableName(), nodeTemplates, repeat.tagName());
            }
            case NodeTemplate.Text text -> text;
            case NodeTemplate.Include include -> include;
        };

        return rewrite.rewrite(transformed, execution, context);
    }

    private List<NodeTemplate> mapList(
            List<NodeTemplate> nodes,
            RenderingExecution execution,
            TemplateRewrite rewrite,
            TraversalContext context
    ) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }

        List<NodeTemplate> result = new ArrayList<>(nodes.size());

        for (NodeTemplate child : nodes) {
            NodeTemplate mapped = traverse(child, execution, rewrite, context);
            if (mapped != null) {
                result.add(mapped);
            }
        }

        return List.copyOf(result);
    }
}
