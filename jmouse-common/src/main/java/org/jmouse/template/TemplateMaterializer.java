package org.jmouse.template;

import org.jmouse.core.Verify;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.PropertyPath;
import org.jmouse.core.access.ValueNavigator;
import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;
import org.jmouse.dom.node.ElementNode;
import org.jmouse.dom.node.TextNode;

import java.util.*;

/**
 * Materializes a {@link NodeTemplate} into a {@link Node} tree.
 */
public interface TemplateMaterializer {

    /**
     * Materialize a blueprint into a node tree.
     *
     * @param blueprint blueprint
     * @param execution execution context
     * @return node root
     */
    Node materialize(NodeTemplate blueprint, RenderingExecution execution);

    /**
     * Standard implementation based on {@link ValueNavigator} and {@link PropertyPath}.
     */
    final class Implementation implements TemplateMaterializer {

        private final ValueResolver      valueResolver      = new DefaultValueResolver(new PathValueResolver());
        private final PredicateEvaluator predicateEvaluator = new PredicateEvaluator(valueResolver);

        @Override
        public Node materialize(NodeTemplate template, RenderingExecution execution) {
            Verify.nonNull(template, "template");
            Verify.nonNull(execution, "execution");
            return materializeInternal(template, execution);
        }

        private Node materializeInternal(NodeTemplate template, RenderingExecution execution) {
            return switch (template) {
                case NodeTemplate.Element element           -> materializeElement(element, execution);
                case NodeTemplate.Text text                 -> materializeText(text, execution);
                case NodeTemplate.Conditional conditional   -> materializeConditional(conditional, execution);
                case NodeTemplate.Repeat repeat             -> materializeRepeat(repeat, execution);
                case NodeTemplate.Include include           -> materializeInclude(include, execution);
                case null -> null;
            };
        }

        private Node materializeElement(NodeTemplate.Element element, RenderingExecution execution) {
            ElementNode node = new ElementNode(toTagName(element.tagName()));

            applyAttributes(node, element.attributes(), execution);

            DirectiveOutcome outcome = applyDirectives(node, element.directives(), execution);

            if (outcome.isOmitted()) {
                return null;
            }

            ElementNode contentNode = outcome.node();

            for (NodeTemplate child : element.children()) {
                Node childNode = materializeInternal(child, execution);
                if (childNode != null) {
                    contentNode.append(childNode);
                }
            }

            return outcome.root();
        }

        private Node materializeText(NodeTemplate.Text text, RenderingExecution execution) {
            Object value = valueResolver.resolve(text.value(), execution);
            return new TextNode(value == null ? "" : String.valueOf(value));
        }

        private Node materializeConditional(NodeTemplate.Conditional conditional, RenderingExecution execution) {
            boolean predicate = predicateEvaluator.evaluate(conditional.predicate(), execution);

            List<NodeTemplate> branch = predicate ? conditional.whenTrue() : conditional.whenFalse();

            if (branch.isEmpty()) {
                return new TextNode("");
            }

            if (branch.size() == 1) {
                return materializeInternal(branch.getFirst(), execution);
            }

            ElementNode container = new ElementNode(TagName.DIV);

            for (NodeTemplate child : branch) {
                container.append(materializeInternal(child, execution));
            }

            return container;
        }

        private Node materializeRepeat(NodeTemplate.Repeat repeat, RenderingExecution execution) {
            Object         collectionValue    = valueResolver.resolve(repeat.collection(), execution);
            ObjectAccessor collectionAccessor = execution.accessorWrapper().wrapIfNecessary(collectionValue);

            if (!(collectionAccessor.isCollection() || collectionAccessor.isList() || collectionAccessor.isMap())) {
                return new TextNode("");
            }

            ElementNode                 container = new ElementNode(toTagName(repeat.tagName()));
            Set<Object>                 keys      = collectionAccessor.keySet();
            Map<String, ObjectAccessor> variables = execution.variables();

            for (Object key : keys) {
                Object         entry         = collectionAccessor.get(key);
                ObjectAccessor entryAccessor = execution.accessorWrapper().wrapIfNecessary(entry);

                variables.put(repeat.itemVariableName(), entryAccessor);

                for (NodeTemplate bodyNode : repeat.body()) {
                    container.append(materializeInternal(bodyNode, execution));
                }

                variables.remove(repeat.itemVariableName());
            }

            return container;
        }

        private Node materializeInclude(NodeTemplate.Include include, RenderingExecution execution) {
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

            NodeTemplate blueprint = execution.resolver().resolve(key, nested);

            return materializeInternal(blueprint, nested);
        }

        private DirectiveOutcome applyDirectives(
                ElementNode node,
                List<NodeDirective> directives,
                RenderingExecution execution
        ) {
            if (directives == null || directives.isEmpty()) {
                return DirectiveOutcome.keep(node);
            }

            Node root = node;

            for (NodeDirective directive : directives) {
                switch (directive) {
                    case NodeDirective.OmitIf omitIf -> {
                        if (predicateEvaluator.evaluate(omitIf.predicate(), execution)) {
                            return DirectiveOutcome.omit();
                        }
                    }
                    case NodeDirective.SetAttributeIf attributeIf -> {
                        if (predicateEvaluator.evaluate(attributeIf.predicate(), execution)) {
                            Object value = valueResolver.resolve(attributeIf.value(), execution);
                            if (value != null) {
                                node.addAttribute(attributeIf.name(), String.valueOf(value));
                            }
                        }
                    }
                    case NodeDirective.RemoveAttributeIf removeIf -> {
                        if (predicateEvaluator.evaluate(removeIf.predicate(), execution)) {
                            node.getAttributes().remove(removeIf.attributeName());
                        }
                    }
                    case NodeDirective.AddClassIf classIf -> {
                        if (predicateEvaluator.evaluate(classIf.predicate(), execution)) {
                            Object value = valueResolver.resolve(classIf.classValue(), execution);
                            if (value != null) {
                                String additional = String.valueOf(value).trim();
                                Node.addClass(node, additional);
                            }
                        }
                    }
                    case NodeDirective.WrapIf wrapIf -> {
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
                Map<String, ValueExpression> attributes,
                RenderingExecution execution
        ) {
            for (Map.Entry<String, ValueExpression> entry : attributes.entrySet()) {
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

        public boolean evaluate(TemplatePredicate predicate, RenderingExecution execution) {
            return switch (predicate) {
                case TemplatePredicate.BooleanValue booleanValuePredicate ->
                        asBoolean(resolver.resolve(booleanValuePredicate.value(), execution));
                case TemplatePredicate.Present presentPredicate ->
                        isPresent(resolver.resolve(presentPredicate.value(), execution));
                case TemplatePredicate.Equality equalityPredicate ->
                        Objects.equals(
                                resolver.resolve(equalityPredicate.left(), execution),
                                resolver.resolve(equalityPredicate.right(), execution)
                        );
                case TemplatePredicate.Not notPredicate ->
                        !evaluate(notPredicate.inner(), execution);
                case TemplatePredicate.All allPredicate -> {
                    for (TemplatePredicate inner : allPredicate.predicates()) {
                        if (!evaluate(inner, execution)) {
                            yield false;
                        }
                    }
                    yield true;
                }
                case TemplatePredicate.Contains containsPredicate -> contains(
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
