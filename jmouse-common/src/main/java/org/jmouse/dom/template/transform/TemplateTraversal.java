package org.jmouse.dom.template.transform;

import org.jmouse.core.Verify;
import org.jmouse.dom.template.*;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.dom.template.NodeTemplate.*;

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
        NodeTemplate rewritten = rewrite.rewrite(node, execution, context);

        if (rewritten == null) {
            return null;
        }

        return switch (rewritten) {
            case Element element -> {
                context.enter(element);

                List<NodeTemplate> children = new ArrayList<>(element.children().size());

                for (NodeTemplate child : element.children()) {
                    NodeTemplate mapped = traverse(child, execution, rewrite, context);
                    if (mapped != null) {
                        children.add(mapped);
                    }
                }

                context.exit();

                yield new Element(
                        element.tagName(),
                        element.attributes(),
                        List.copyOf(children),
                        List.copyOf(element.directives())
                );
            }
            case Conditional conditional -> {
                List<NodeTemplate> ift = mapList(conditional.whenTrue(), execution, rewrite, context);
                List<NodeTemplate> iff = mapList(conditional.whenFalse(), execution, rewrite, context);
                yield new Conditional(conditional.predicate(), ift, iff);
            }
            case Repeat repeat -> {
                List<NodeTemplate> body = mapList(repeat.body(), execution, rewrite, context);
                yield new Repeat(repeat.collection(), repeat.itemVariableName(), body);
            }
            case Text text -> text;
            case Include include -> include;
        };
    }

    private List<NodeTemplate> mapList(
            List<NodeTemplate> nodes,
            RenderingExecution execution,
            TemplateRewrite rewrite,
            TraversalContext context
    ) {
        List<NodeTemplate> result = new ArrayList<>(nodes.size());

        for (NodeTemplate node : nodes) {
            NodeTemplate mapped = traverse(node, execution, rewrite, context);
            if (mapped != null) {
                result.add(mapped);
            }
        }

        return List.copyOf(result);
    }
}
