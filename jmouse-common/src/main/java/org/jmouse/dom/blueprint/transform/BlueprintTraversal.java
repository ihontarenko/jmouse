package org.jmouse.dom.blueprint.transform;

import org.jmouse.core.Verify;
import org.jmouse.dom.blueprint.*;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.dom.blueprint.Blueprint.*;

public final class BlueprintTraversal {

    public Blueprint traverse(
            Blueprint root,
            RenderingExecution execution,
            BlueprintRewrite rewrite
    ) {
        Verify.nonNull(execution, "execution");
        Verify.nonNull(rewrite, "rewrite");
        return traverse(root, execution, rewrite, new TraversalContext());
    }

    public Blueprint traverse(
            Blueprint node,
            RenderingExecution execution,
            BlueprintRewrite rewrite,
            TraversalContext context
    ) {
        Blueprint rewritten = rewrite.rewrite(node, execution, context);

        if (rewritten == null) {
            return null;
        }

        return switch (rewritten) {
            case ElementBlueprint element -> {
                context.enter(element);

                List<Blueprint> children = new ArrayList<>(element.children().size());
                for (Blueprint child : element.children()) {
                    Blueprint mapped = traverse(child, execution, rewrite, context);
                    if (mapped != null) {
                        children.add(mapped);
                    }
                }

                context.exit();

                yield new ElementBlueprint(
                        element.tagName(),
                        element.attributes(),
                        List.copyOf(children),
                        List.copyOf(element.directives())
                );
            }
            case ConditionalBlueprint conditional -> {
                List<Blueprint> whenTrue  = mapList(conditional.whenTrue(), execution, rewrite, context);
                List<Blueprint> whenFalse = mapList(conditional.whenFalse(), execution, rewrite, context);
                yield new ConditionalBlueprint(conditional.predicate(), whenTrue, whenFalse);
            }
            case RepeatBlueprint repeat -> {
                List<Blueprint> body = mapList(repeat.body(), execution, rewrite, context);
                yield new RepeatBlueprint(repeat.collection(), repeat.itemVariableName(), body);
            }
            case TextBlueprint text -> text;
            case IncludeBlueprint include -> include; // include розкриваємо у rewrite
        };
    }

    private List<Blueprint> mapList(
            List<Blueprint> nodes,
            RenderingExecution execution,
            BlueprintRewrite rewrite,
            TraversalContext context
    ) {
        List<Blueprint> result = new ArrayList<>(nodes.size());

        for (Blueprint node : nodes) {
            Blueprint mapped = traverse(node, execution, rewrite, context);
            if (mapped != null) {
                result.add(mapped);
            }
        }

        return List.copyOf(result);
    }
}
