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

import java.util.*;

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
     * Standard implementation based on {@link ValueNavigator} and {@link PropertyPath}.
     */
    final class Implementation implements BlueprintMaterializer {

        private final ValueResolver      valueResolver      = new DefaultValueResolver(new PathValueResolver());
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
                case null -> null;
            };
        }

        private Node materializeElement(Blueprint.ElementBlueprint element, RenderingExecution execution) {
            ElementNode node = new ElementNode(toTagName(element.tagName()));

            applyAttributes(node, element.attributes(), execution);

            DirectiveOutcome outcome = applyDirectives(node, element.directives(), execution);

            if (outcome.isOmitted()) {
                return null;
            }

            ElementNode contentNode = outcome.node();

            for (Blueprint child : element.children()) {
                Node childNode = materializeInternal(child, execution);
                if (childNode != null) {
                    contentNode.append(childNode);
                }
            }

            return outcome.root();
        }

        private Node materializeText(Blueprint.TextBlueprint text, RenderingExecution execution) {
            Object value = valueResolver.resolve(text.value(), execution);
            return new TextNode(value == null ? "" : String.valueOf(value));
        }

        private Node materializeConditional(Blueprint.ConditionalBlueprint conditional, RenderingExecution execution) {
            boolean predicate = predicateEvaluator.evaluate(conditional.predicate(), execution);

            List<Blueprint> branch = predicate ? conditional.whenTrue() : conditional.whenFalse();

            if (branch.isEmpty()) {
                return new TextNode("");
            }

            if (branch.size() == 1) {
                return materializeInternal(branch.getFirst(), execution);
            }

            ElementNode container = new ElementNode(TagName.DIV);

            for (Blueprint child : branch) {
                container.append(materializeInternal(child, execution));
            }

            return container;
        }

        private Node materializeRepeat(Blueprint.RepeatBlueprint repeat, RenderingExecution execution) {
            Object         collectionValue    = valueResolver.resolve(repeat.collection(), execution);
            ObjectAccessor collectionAccessor = execution.accessorWrapper().wrapIfNecessary(collectionValue);

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
            Object keyValue = valueResolver.resolve(include.blueprintKey(), execution);

            if (keyValue == null) {
                return new TextNode("");
            }

            String         key        = String.valueOf(keyValue);
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

            Blueprint blueprint = execution.resolver().resolve(key, nested);

            return materializeInternal(blueprint, nested);
        }

        private DirectiveOutcome applyDirectives(
                ElementNode node,
                List<BlueprintDirective> directives,
                RenderingExecution execution
        ) {
            if (directives == null || directives.isEmpty()) {
                return DirectiveOutcome.keep(node);
            }

            Node root = node;

            for (BlueprintDirective directive : directives) {
                switch (directive) {
                    case BlueprintDirective.OmitIf omitIf -> {
                        if (predicateEvaluator.evaluate(omitIf.predicate(), execution)) {
                            return DirectiveOutcome.omit();
                        }
                    }
                    case BlueprintDirective.SetAttributeIf attributeIf -> {
                        if (predicateEvaluator.evaluate(attributeIf.predicate(), execution)) {
                            Object value = valueResolver.resolve(attributeIf.value(), execution);
                            if (value != null) {
                                node.addAttribute(attributeIf.name(), String.valueOf(value));
                            }
                        }
                    }
                    case BlueprintDirective.RemoveAttributeIf removeIf -> {
                        if (predicateEvaluator.evaluate(removeIf.predicate(), execution)) {
                            node.getAttributes().remove(removeIf.attributeName());
                        }
                    }
                    case BlueprintDirective.AddClassIf classIf -> {
                        if (predicateEvaluator.evaluate(classIf.predicate(), execution)) {
                            Object value = valueResolver.resolve(classIf.classValue(), execution);
                            if (value != null) {
                                String additional = String.valueOf(value).trim();
                                Node.addClass(node, additional);
                            }
                        }
                    }
                    case BlueprintDirective.WrapIf wrapIf -> {
                        if (predicateEvaluator.evaluate(wrapIf.predicate(), execution)) {
                            ElementNode wrapper = new ElementNode(toTagName(wrapIf.wrapperTagName()));
                            applyAttributes(wrapper, wrapIf.wrapperAttributes(), execution);
                            node.wrap(wrapper);
                            root = wrapper;
                        }
                    }
                }
            }

            return DirectiveOutcome.wrapped(node, root);
        }

        private void applyAttributes(
                ElementNode node,
                Map<String, BlueprintValue> attributes,
                RenderingExecution execution
        ) {
            for (Map.Entry<String, BlueprintValue> entry : attributes.entrySet()) {
                String name  = entry.getKey();
                Object value = valueResolver.resolve(entry.getValue(), execution);

                if (value == null) {
                    continue;
                }

                node.addAttribute(name, String.valueOf(value));
            }
        }

        private TagName toTagName(String tagName) {
            return TagName.valueOf(tagName.toUpperCase());
        }
    }



    /**
     * The result of directive application.
     *
     * <p>Directives may:</p>
     * <ul>
     *   <li>omit the node entirely</li>
     *   <li>wrap the node (root changes, but content node stays the same)</li>
     *   <li>mutate attributes/classes</li>
     * </ul>
     */
    record DirectiveOutcome(
            boolean omitted,
            ElementNode node,
            Node root
    ) {
        static DirectiveOutcome keep(ElementNode node) {
            return new DirectiveOutcome(false, node, node);
        }

        static DirectiveOutcome omit() {
            return new DirectiveOutcome(true, null, null);
        }

        static DirectiveOutcome wrapped(ElementNode node, Node root) {
            return new DirectiveOutcome(false, node, root);
        }

        boolean isOmitted() {
            return omitted;
        }
    }

    /**
     * Evaluates blueprint predicates.
     */
    final class PredicateEvaluator {

        private final ValueResolver resolver;

        public PredicateEvaluator(ValueResolver resolver) {
            this.resolver = Verify.nonNull(resolver, "resolver");
        }

        public boolean evaluate(BlueprintPredicate predicate, RenderingExecution execution) {
            return switch (predicate) {
                case BlueprintPredicate.BooleanValuePredicate booleanValuePredicate ->
                        asBoolean(resolver.resolve(booleanValuePredicate.value(), execution));
                case BlueprintPredicate.PresentPredicate presentPredicate ->
                        isPresent(resolver.resolve(presentPredicate.value(), execution));
                case BlueprintPredicate.EqualityPredicate equalityPredicate ->
                        Objects.equals(
                                resolver.resolve(equalityPredicate.left(), execution),
                                resolver.resolve(equalityPredicate.right(), execution)
                        );
                case BlueprintPredicate.NotPredicate notPredicate ->
                        !evaluate(notPredicate.inner(), execution);
                case BlueprintPredicate.AllPredicate allPredicate -> {
                    for (BlueprintPredicate inner : allPredicate.predicates()) {
                        if (!evaluate(inner, execution)) {
                            yield false;
                        }
                    }
                    yield true;
                }
                case BlueprintPredicate.ContainsPredicate containsPredicate -> contains(
                        resolver.resolve(containsPredicate.collection(), execution),
                        resolver.resolve(containsPredicate.value(), execution)
                );
                default -> throw new IllegalStateException("Unexpected predicate: " + predicate);
            };
        }

        private boolean isPresent(Object value) {
            return switch (value) {
                case null -> false;
                case String string -> !string.trim().isEmpty();
                case Collection<?> collection -> !collection.isEmpty();
                case Map<?, ?> map -> !map.isEmpty();
                default -> true;
            };
        }

        private boolean asBoolean(Object value) {
            return switch (value) {
                case null -> false;
                case Boolean booleanValue -> booleanValue;
                case Number number -> number.intValue() != 0;
                default -> Boolean.parseBoolean(String.valueOf(value));
            };
        }

        private boolean contains(Object collectionValue, Object searchedValue) {
            if (collectionValue == null || searchedValue == null) {
                return false;
            }

            if (collectionValue.getClass().isArray()) {
                return contains(List.of(((Object[]) collectionValue)), searchedValue);
            }

            return switch (collectionValue) {
                case Collection<?> collection -> collection.contains(searchedValue);
                case Map<?, ?> map -> map.containsKey(searchedValue);
                case String text -> text.contains(String.valueOf(searchedValue));
                default -> Objects.equals(collectionValue, searchedValue);
            };

        }
    }
}
