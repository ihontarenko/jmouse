package org.jmouse.dom.blueprint;

import org.jmouse.core.Verify;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.PropertyPath;
import org.jmouse.core.access.ValueNavigator;
import org.jmouse.core.access.accessor.ScalarValueAccessor;
import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;
import org.jmouse.dom.node.ElementNode;
import org.jmouse.dom.node.TextNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Materializes a {@link Blueprint} into a {@link Node} tree.
 */
public interface BlueprintMaterializer {

    /**
     * Materialize a blueprint into a node tree.
     *
     * @param blueprint blueprint
     * @param execution execution context
     * @return node root
     */
    Node materialize(Blueprint blueprint, RenderingExecution execution);

    /**
     * Standard implementation based on {@link org.jmouse.core.access.ValueNavigator} and {@link PropertyPath}.
     */
    final class Implementation implements BlueprintMaterializer {

        private final PathValueResolver  valueResolver      = new PathValueResolver();
        private final PredicateEvaluator predicateEvaluator = new PredicateEvaluator(valueResolver);
        private final BlueprintCatalog   catalog;

        public Implementation(BlueprintCatalog catalog) {
            this.catalog = catalog;
        }

        @Override
        public Node materialize(Blueprint blueprint, RenderingExecution execution) {
            Verify.nonNull(blueprint, "blueprint");
            Verify.nonNull(execution, "execution");
            return materializeInternal(blueprint, execution);
        }

        private Node materializeInternal(Blueprint blueprint, RenderingExecution execution) {
            return switch (blueprint) {
                case Blueprint.ElementBlueprint element -> materializeElement(element, execution);
                case Blueprint.TextBlueprint text -> materializeText(text, execution);
                case Blueprint.ConditionalBlueprint conditional -> materializeConditional(conditional, execution);
                case Blueprint.RepeatBlueprint repeat -> materializeRepeat(repeat, execution);
                case Blueprint.IncludeBlueprint include -> materializeInclude(include, execution);
                case null -> throw new IllegalStateException("Unsupported blueprint type.");
            };
        }

        private Node materializeElement(Blueprint.ElementBlueprint element, RenderingExecution execution) {
            ElementNode node = new ElementNode(toTagName(element.tagName()));

            applyAttributes(node, element.attributes(), execution);

            for (Blueprint child : element.children()) {
                node.append(materializeInternal(child, execution));
            }

            return node;
        }

        private Node materializeText(Blueprint.TextBlueprint text, RenderingExecution execution) {
            Object value = resolveValue(text.value(), execution);
            return new TextNode(value == null ? "" : String.valueOf(value));
        }

        private Node materializeConditional(Blueprint.ConditionalBlueprint conditional, RenderingExecution execution) {
            boolean predicate = predicateEvaluator.evaluate(conditional.predicate(), execution);

            List<Blueprint> branch = predicate ? conditional.whenTrue() : conditional.whenFalse();

            if (branch.isEmpty()) {
                return new TextNode("");
            }

            if (branch.size() == 1) {
                return materializeInternal(branch.get(0), execution);
            }

            // Container is a minimal structural fallback.
            ElementNode container = new ElementNode(TagName.DIV);
            for (Blueprint child : branch) {
                container.append(materializeInternal(child, execution));
            }
            return container;
        }

        private Node materializeRepeat(Blueprint.RepeatBlueprint repeat, RenderingExecution execution) {
            Object         collectionValue    = resolveValue(repeat.collection(), execution);
            ObjectAccessor collectionAccessor = execution.accessorWrapper().wrap(collectionValue);

            if (!(collectionAccessor.isCollection() || collectionAccessor.isList() || collectionAccessor.isMap())) {
                return new TextNode("");
            }

            ElementNode                 container = new ElementNode(TagName.DIV);
            Set<Object>                 keys      = collectionAccessor.keySet();
            Map<String, ObjectAccessor> variables = execution.variables();

            for (Object key : keys) {
                Object         item           = collectionAccessor.get(key);
                ObjectAccessor objectAccessor = execution.accessorWrapper().wrapIfNecessary(item);

                variables.put(repeat.itemVariableName(), objectAccessor);

                for (Blueprint bodyNode : repeat.body()) {
                    container.append(materializeInternal(bodyNode, execution));
                }

                variables.remove(repeat.itemVariableName());
            }

            return container;
        }

        private Node materializeInclude(Blueprint.IncludeBlueprint include, RenderingExecution execution) {
            Object keyValue = resolveValue(include.blueprintKey(), execution);

            if (keyValue == null) {
                return new TextNode("");
            }

            String         key           = String.valueOf(keyValue);
            Object         modelValue    = resolveValue(include.model(), execution);
            ObjectAccessor modelAccessor = execution.accessorWrapper().wrapIfNecessary(modelValue);

            RenderingExecution nested = new RenderingExecution(
                    execution.accessorWrapper(),
                    modelAccessor,
                    execution.request(),
                    execution.valueNavigator()
            );
            nested.variables().putAll(execution.variables());
            nested.diagnostics().putAll(execution.diagnostics());

            Blueprint blueprint = catalog.resolve(key);

            return materializeInternal(blueprint, nested);
        }

        private void applyAttributes(
                ElementNode node,
                Map<String, BlueprintValue> attributes,
                RenderingExecution execution
        ) {
            for (Map.Entry<String, BlueprintValue> entry : attributes.entrySet()) {
                String name  = entry.getKey();
                Object value = resolveValue(entry.getValue(), execution);

                if (value == null) {
                    continue;
                }

                node.addAttribute(name, String.valueOf(value));
            }
        }

        private Object resolveValue(BlueprintValue value, RenderingExecution execution) {
            if (value instanceof BlueprintValue.ConstantValue(Object constantValue)) {
                return constantValue;
            }
            if (value instanceof BlueprintValue.PathValue(String pathExpression)) {
                return valueResolver.resolve(pathExpression, execution);
            }
            if (value instanceof BlueprintValue.RequestAttributeValue(String name)) {
                return execution.request().attributes().get(name);
            }
            throw new IllegalStateException("Unsupported BlueprintValue type: " + value.getClass().getName());
        }

        private TagName toTagName(String tagName) {
            return TagName.valueOf(tagName.toUpperCase());
        }
    }

    /**
     * Resolves path expressions against root accessor and scoped variables using {@link PropertyPath}.
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>If the first path segment matches a scoped variable name, resolve against that variable accessor.</li>
     *   <li>Otherwise resolve against the root accessor.</li>
     *   <li>Resolution is delegated to {@link org.jmouse.core.access.ValueNavigator}.</li>
     * </ul>
     */
    final class PathValueResolver {

        public Object resolve(String expression, RenderingExecution execution) {
            Verify.nonNull(expression, "expression");
            Verify.nonNull(execution, "execution");

            String trimmed = expression.trim();

            if (trimmed.isEmpty()) {
                return null;
            }

            ValueNavigator navigator = execution.valueNavigator();
            PropertyPath   path      = PropertyPath.forPath(trimmed);
            String         head      = path.head().toString();
            ObjectAccessor scoped    = execution.variables().get(head);

            if (scoped != null) {
                PropertyPath remainder = path.sub(1);
                if (remainder.isEmpty()) {
                    return unwrap(scoped);
                }
                return unwrap(navigator.navigate(scoped, remainder.toString()));
            }

            return unwrap(navigator.navigate(execution.rootAccessor(), path.toString()));
        }

        private Object unwrap(Object value) {
            if (value instanceof ScalarValueAccessor scalar) {
                return scalar.unwrap();
            }
            if (value instanceof ObjectAccessor accessor) {
                // For non-scalar results, return accessor itself.
                // Attributes and text will call String.valueOf(...) which becomes stable.
                return accessor;
            }
            return value;
        }
    }

    /**
     * Evaluates blueprint predicates.
     */
    final class PredicateEvaluator {

        private final PathValueResolver resolver;

        public PredicateEvaluator(PathValueResolver resolver) {
            this.resolver = Verify.nonNull(resolver, "resolver");
        }

        public boolean evaluate(BlueprintPredicate predicate, RenderingExecution execution) {
            if (predicate instanceof BlueprintPredicate.PathBooleanPredicate(String path)) {
                Object value = resolver.resolve(path, execution);
                return asBoolean(value);
            }
            throw new IllegalStateException("Unsupported predicate type: " + predicate.getClass().getName());
        }

        private boolean asBoolean(Object value) {
            return switch (value) {
                case null -> false;
                case Boolean booleanValue -> booleanValue;
                case Number number -> number.intValue() != 0;
                default -> Boolean.parseBoolean(String.valueOf(value));
            };
        }
    }
}
