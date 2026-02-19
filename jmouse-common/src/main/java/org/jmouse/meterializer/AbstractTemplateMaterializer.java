package org.jmouse.meterializer;

import org.jmouse.core.Verify;
import org.jmouse.core.access.ObjectAccessor;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractTemplateMaterializer<R> implements TemplateMaterializer<R> {

    protected final ValueResolver      valueResolver;
    protected final PredicateEvaluator predicateEvaluator;

    protected AbstractTemplateMaterializer(ValueResolver valueResolver) {
        this.valueResolver = Verify.nonNull(valueResolver, "valueResolver");
        this.predicateEvaluator = new PredicateEvaluator(this.valueResolver);
    }

    @Override
    public final R materialize(NodeTemplate template, RenderingExecution execution) {
        Verify.nonNull(template, "template");
        Verify.nonNull(execution, "execution");
        return materializeInternal(template, execution);
    }

    protected R materializeInternal(NodeTemplate template, RenderingExecution execution) {
        return switch (template) {
            case NodeTemplate.Element element -> materializeElement(element, execution);
            case NodeTemplate.Text text -> materializeText(text, execution);
            case NodeTemplate.Conditional conditional -> materializeConditional(conditional, execution);
            case NodeTemplate.Repeat repeat -> materializeRepeat(repeat, execution);
            case NodeTemplate.Include include -> materializeInclude(include, execution);
            case null -> null;
        };
    }

    protected R materializeText(NodeTemplate.Text text, RenderingExecution execution) {
        Object value = valueResolver.resolve(text.value(), execution);
        return createTextNode(value == null ? "" : String.valueOf(value));
    }

    protected R materializeConditional(NodeTemplate.Conditional conditional, RenderingExecution execution) {
        boolean predicate = predicateEvaluator.evaluate(conditional.predicate(), execution);

        List<NodeTemplate> branch = predicate ? conditional.whenTrue() : conditional.whenFalse();

        if (branch.isEmpty()) {
            return emptyNode();
        }

        if (branch.size() == 1) {
            return materializeInternal(branch.getFirst(), execution);
        }

        R container = createContainerNode();

        for (NodeTemplate child : branch) {
            R childNode = materializeInternal(child, execution);
            if (childNode != null) {
                appendChild(container, childNode);
            }
        }

        return container;
    }

    protected R materializeRepeat(NodeTemplate.Repeat repeat, RenderingExecution execution) {
        Object         collectionValue    = valueResolver.resolve(repeat.collection(), execution);
        ObjectAccessor collectionAccessor = execution.accessorWrapper().wrapIfNecessary(collectionValue);

        if (!(collectionAccessor.isCollection() || collectionAccessor.isList() || collectionAccessor.isMap())) {
            return emptyNode();
        }

        R container = createElementNode(repeat.tagName());

        Set<Object>                 keys      = collectionAccessor.keySet();
        Map<String, ObjectAccessor> variables = execution.variables();

        for (Object key : keys) {
            Object         entry         = collectionAccessor.get(key);
            ObjectAccessor entryAccessor = execution.accessorWrapper().wrapIfNecessary(entry);

            variables.put(repeat.itemVariableName(), entryAccessor);

            for (NodeTemplate bodyNode : repeat.body()) {
                R child = materializeInternal(bodyNode, execution);
                if (child != null) {
                    appendChild(container, child);
                }
            }

            variables.remove(repeat.itemVariableName());
        }

        return container;
    }

    protected R materializeInclude(NodeTemplate.Include include, RenderingExecution execution) {
        Object keyValue = valueResolver.resolve(include.blueprintKey(), execution);

        if (keyValue == null) {
            return emptyNode();
        }

        String key = String.valueOf(keyValue);

        Object         modelValue = valueResolver.resolve(include.model(), execution);
        ObjectAccessor accessor   = execution.accessorWrapper().wrapIfNecessary(modelValue);

        RenderingExecution nested = new RenderingExecution(
                execution.accessorWrapper(),
                accessor,
                execution.request(),
                execution.valueNavigator(),
                execution.resolver()
        );
        nested.variables().putAll(execution.variables());

        NodeTemplate resolved = execution.resolver().resolve(key, nested);

        return materializeInternal(resolved, nested);
    }

    protected abstract R createElementNode(String tagName);
    protected abstract R createTextNode(String text);
    protected abstract R createContainerNode();
    protected abstract R emptyNode();
    protected abstract void appendChild(R parent, R child);
    protected abstract R materializeElement(NodeTemplate.Element element, RenderingExecution execution);

}

