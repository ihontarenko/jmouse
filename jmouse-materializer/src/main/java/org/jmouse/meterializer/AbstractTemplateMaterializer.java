package org.jmouse.meterializer;

import org.jmouse.core.Verify;
import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.ValueNavigator;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jmouse.core.Verify.nonNull;
import static org.jmouse.util.Strings.isNotEmpty;

/**
 * Base {@link TemplateMaterializer} with built-in traversal algorithm over {@link NodeTemplate}.
 *
 * <p>
 * This class is format-agnostic: it knows how to walk template structures (element/text/conditional/repeat/include)
 * and delegates concrete node creation and mutation to subclass hooks (create/set/append/wrap, etc.).
 * </p>
 *
 * <p>
 * Core responsibilities:
 * </p>
 * <ul>
 *   <li>value resolution via {@link ValueResolver}</li>
 *   <li>predicate evaluation via {@link PredicateEvaluator}</li>
 *   <li>directive application via {@link NodeDirective} set</li>
 *   <li>standard branching semantics for conditional/repeat/include</li>
 * </ul>
 *
 * @param <R> concrete node representation (DOM, XML, HTML string builder nodes, etc.)
 */
public abstract class AbstractTemplateMaterializer<R> implements TemplateMaterializer<R> {

    /**
     * Resolves runtime values from {@link ValueExpression}s. 🧩
     */
    protected final ValueResolver valueResolver;

    /**
     * Evaluates template predicates using the configured {@link #valueResolver}. 🧠
     */
    protected final PredicateEvaluator predicateEvaluator;

    /**
     * Creates a base materializer using the given {@link ValueResolver}.
     *
     * @param valueResolver resolver used for all value expression evaluation
     */
    protected AbstractTemplateMaterializer(ValueResolver valueResolver) {
        this.valueResolver = nonNull(valueResolver, "valueResolver");
        this.predicateEvaluator = new PredicateEvaluator(this.valueResolver);
    }

    /**
     * Materializes the given template root into a concrete result.
     *
     * <p>
     * Performs argument validation and delegates to {@link #materializeInternal(NodeTemplate, RenderingExecution)}.
     * </p>
     *
     * @param template template root node
     * @param execution active rendering execution context
     * @return materialized result (may be {@code null} depending on node type/outcome)
     */
    @Override
    public final R materialize(NodeTemplate template, RenderingExecution execution) {
        return materializeInternal(nonNull(template, "template"), nonNull(execution, "execution"));
    }

    /**
     * Dispatches materialization based on the concrete {@link NodeTemplate} kind.
     *
     * @param template template node (may be {@code null})
     * @param execution active rendering execution context
     * @return materialized node or {@code null}
     */
    protected R materializeInternal(NodeTemplate template, RenderingExecution execution) {
        return switch (template) {
            case NodeTemplate.Element element           -> materializeElement(element, execution);
            case NodeTemplate.Text text                 -> materializeText(text, execution);
            case NodeTemplate.Conditional conditional   -> materializeConditional(conditional, execution);
            case NodeTemplate.Repeat repeat             -> materializeRepeat(repeat, execution);
            case NodeTemplate.Include include           -> materializeInclude(include, execution);
            case null -> null;
        };
    }

    /**
     * Materializes a text template by resolving its value and converting it to a string.
     *
     * <p>{@code null} resolved values are converted to an empty string.</p>
     *
     * @param text text template node
     * @param execution active rendering execution context
     * @return created text node
     */
    protected R materializeText(NodeTemplate.Text text, RenderingExecution execution) {
        Object value = valueResolver.resolve(text.value(), execution);
        return createTextNode(value == null ? "" : String.valueOf(value));
    }

    /**
     * Materializes a conditional template by selecting a branch using its predicate.
     *
     * <p>
     * Branch semantics:
     * </p>
     * <ul>
     *   <li>empty branch → {@link #emptyNode()}</li>
     *   <li>single node → materialize directly</li>
     *   <li>multiple nodes → materialize into a container and append children</li>
     * </ul>
     *
     * @param conditional conditional template node
     * @param execution active rendering execution context
     * @return materialized branch result
     */
    protected R materializeConditional(NodeTemplate.Conditional conditional, RenderingExecution execution) {
        boolean            predicate = predicateEvaluator.evaluate(conditional.predicate(), execution);
        List<NodeTemplate> branch    = predicate ? conditional.branchA() : conditional.branchB();

        if (branch.isEmpty()) {
            return emptyNode();
        }

        if (branch.size() == 1) {
            return materializeInternal(branch.getFirst(), execution);
        }

        String containerName = conditional.tagName();
        R      container     = isNotEmpty(containerName) ? createElementNode(containerName) : createContainerNode();

        for (NodeTemplate child : branch) {
            R childNode = materializeInternal(child, execution);
            if (childNode != null) {
                appendChild(container, childNode);
            }
        }

        return container;
    }

    /**
     * Materializes a repeat template by iterating over a resolved container value. 🔁
     *
     * <p>
     * Input is wrapped into an {@link ObjectAccessor} so lists/maps/collections can be iterated uniformly.
     * Each iteration binds the current item accessor into {@link RenderingExecution#variables()}
     * under {@link NodeTemplate.Repeat#itemVariableName()}.
     * </p>
     *
     * <p>
     * If the resolved value is not an iterable container, returns {@link #emptyNode()}.
     * </p>
     *
     * @param repeat repeat template node
     * @param execution active rendering execution context
     * @return container node holding rendered items
     */
    protected R materializeRepeat(NodeTemplate.Repeat repeat, RenderingExecution execution) {
        Object         collectionValue    = valueResolver.resolve(repeat.collection(), execution);
        ObjectAccessor collectionAccessor = execution.accessorWrapper().wrapIfNecessary(collectionValue);

        if (!(collectionAccessor.isCollection() || collectionAccessor.isList() || collectionAccessor.isMap())) {
            return emptyNode();
        }

        R container = isNotEmpty(repeat.tagName()) ? createElementNode(repeat.tagName()) : createContainerNode();

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

    /**
     * Materializes an include template by resolving a referenced template and rendering it
     * under a nested execution context. 🧬
     *
     * <p>
     * Steps:
     * </p>
     * <ul>
     *   <li>resolve {@code templateReference}; if {@code null} → {@link #emptyNode()}</li>
     *   <li>resolve {@code model} and wrap it as a new root {@link ObjectAccessor}</li>
     *   <li>create nested {@link RenderingExecution} and copy variables</li>
     *   <li>resolve included template via {@link NodeTemplateResolver}</li>
     *   <li>materialize resolved subtree using nested execution</li>
     * </ul>
     *
     * @param include include template node
     * @param execution active rendering execution context
     * @return materialized included subtree
     */
    protected R materializeInclude(NodeTemplate.Include include, RenderingExecution execution) {
        Object keyValue = valueResolver.resolve(include.templateReference(), execution);

        if (keyValue == null) {
            return emptyNode();
        }

        String key = String.valueOf(keyValue);

        AccessorWrapper wrapper    = execution.accessorWrapper();
        Object          modelValue = valueResolver.resolve(include.model(), execution);
        ObjectAccessor  accessor   = wrapper.wrapIfNecessary(modelValue);

        RenderingExecution nested = new RenderingExecution(
                wrapper,
                accessor,
                execution.request(),
                execution.valueNavigator(),
                execution.resolver()
        );
        nested.variables().putAll(execution.variables());

        NodeTemplate resolved = execution.resolver().resolve(key, nested);

        return materializeInternal(resolved, nested);
    }

    /**
     * Applies resolved attributes to the given node.
     *
     * <p>
     * Skips invalid entries (blank name / null expression) and ignores {@code null} resolved values.
     * Values are converted using {@link String#valueOf(Object)}.
     * </p>
     *
     * @param node target node
     * @param attributes attribute expressions map (name → expression)
     * @param execution active rendering execution context
     */
    protected final void applyAttributes(
            R node,
            Map<String, ValueExpression> attributes,
            RenderingExecution execution
    ) {
        if (attributes == null || attributes.isEmpty()) {
            return;
        }

        for (Map.Entry<String, ValueExpression> entry : attributes.entrySet()) {
            String          name       = entry.getKey();
            ValueExpression expression = entry.getValue();

            if (name == null || name.isBlank() || expression == null) {
                continue;
            }

            Object resolved = valueResolver.resolve(expression, execution);

            if (resolved == null) {
                continue;
            }

            setAttribute(node, name, String.valueOf(resolved));
        }
    }

    /**
     * Applies directives to an element node. 🧰
     *
     * <p>
     * Supported directives:
     * omit, conditional set/remove attribute, conditional class append, conditional wrapping,
     * and dynamic attribute application.
     * </p>
     *
     * @param node target node
     * @param directives directives list
     * @param execution active rendering execution context
     * @return directive outcome (keep / wrap / omit)
     */
    protected final DirectiveOutcome<R> applyDirectives(
            R node,
            List<NodeDirective> directives,
            RenderingExecution execution
    ) {
        if (directives == null || directives.isEmpty()) {
            return DirectiveOutcome.keep(node);
        }

        R root = node;

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
                            setAttribute(node, attributeIf.name(), String.valueOf(value));
                        }
                    }
                }
                case NodeDirective.RemoveAttributeIf removeIf -> {
                    if (predicateEvaluator.evaluate(removeIf.predicate(), execution)) {
                        removeAttribute(node, removeIf.attributeName());
                    }
                }
                case NodeDirective.AddClassIf classIf -> {
                    if (predicateEvaluator.evaluate(classIf.predicate(), execution)) {
                        Object value = valueResolver.resolve(classIf.classValue(), execution);
                        if (value != null) {
                            String classNames = String.valueOf(value).trim();
                            if (!classNames.isEmpty()) {
                                addClass(node, classNames);
                            }
                        }
                    }
                }
                case NodeDirective.WrapIf wrapIf -> {
                    if (predicateEvaluator.evaluate(wrapIf.predicate(), execution)) {
                        R wrapper = wrapElement(node, wrapIf.wrapperTagName());
                        applyAttributes(wrapper, wrapIf.wrapperAttributes(), execution);
                        root = wrapper;
                    }
                }
                case NodeDirective.ApplyAttributes applyAttributes ->
                        applyAttributes(node, applyAttributes, execution);
                case null -> { }
            }
        }

        return DirectiveOutcome.wrapped(node, root);
    }

    /**
     * Applies {@link NodeDirective.ApplyAttributes} by reading attribute pairs from resolved data.
     *
     * <p>
     * Supported sources:
     * </p>
     * <ul>
     *   <li>map: key → value becomes attributeName → attributeValue</li>
     *   <li>collection/list: each item is treated as a record; keys are extracted via {@link ValueNavigator}</li>
     * </ul>
     *
     * @param node target node
     * @param directive apply-attributes directive
     * @param execution active rendering execution context
     */
    protected final void applyAttributes(
            R node,
            NodeDirective.ApplyAttributes directive,
            RenderingExecution execution
    ) {
        Object resolved = valueResolver.resolve(directive.source(), execution);

        if (resolved == null) {
            return;
        }

        ObjectAccessor accessor = execution.accessorWrapper().wrapIfNecessary(resolved);

        if (accessor.length() == 0) {
            return;
        }

        Map<String, ValueExpression> attributes = new java.util.LinkedHashMap<>();

        if (accessor.isMap()) {
            for (Object key : accessor.keySet()) {
                Object valueKey = accessor.get(key);
                String keyValue = String.valueOf(key);

                if (keyValue.isBlank()) {
                    continue;
                }

                attributes.put(keyValue, ValueExpression.constant(valueKey));
            }

            applyAttributes(node, attributes, execution);
            return;
        }

        if (accessor.isCollection() || accessor.isList()) {
            ValueNavigator navigator = execution.valueNavigator();

            for (Object key : accessor.keySet()) {
                if (accessor.get(key) instanceof ObjectAccessor item && !item.isNull()) {
                    Object keyValue = navigator.navigate(item, directive.keyValue());
                    Object valueKey = navigator.navigate(item, directive.valueKey());

                    String attributeName = String.valueOf(keyValue);
                    if (attributeName.isBlank()) {
                        continue;
                    }

                    attributes.put(attributeName, ValueExpression.constant(valueKey));
                }
            }

            applyAttributes(node, attributes, execution);
        }
    }

    /**
     * Sets an attribute on a concrete element representation.
     *
     * @param node target node
     * @param name attribute name
     * @param value attribute value
     */
    protected abstract void setAttribute(R node, String name, String value);

    /**
     * Removes an attribute from a concrete element representation.
     *
     * @param node target node
     * @param name attribute name
     */
    protected abstract void removeAttribute(R node, String name);

    /**
     * Adds one or multiple CSS classes (implementation decides how to parse).
     *
     * @param node target node
     * @param classNames class value (often space-separated)
     */
    protected abstract void addClass(R node, String classNames);

    /**
     * Wraps the current node with a new wrapper element and returns wrapper as a new root.
     *
     * <p>Implementation must also make {@code node} become a child of the wrapper.</p>
     *
     * @param node node to wrap
     * @param wrapperTagName wrapper tag name
     * @return wrapper node (new root)
     */
    protected abstract R wrapElement(R node, String wrapperTagName);

    /**
     * Creates an element node with the given tag name.
     *
     * @param tagName element tag name (e.g. {@code "div"})
     * @return new element node representation
     */
    protected abstract R createElementNode(String tagName);

    /**
     * Creates a text node.
     *
     * @param text text content (never {@code null})
     * @return new text node representation
     */
    protected abstract R createTextNode(String text);

    /**
     * Creates a container node used for multi-node branches. 📦
     *
     * @return container node representation
     */
    protected abstract R createContainerNode();

    /**
     * Returns an "empty" node representation.
     *
     * @return empty node representation
     */
    protected abstract R emptyNode();

    /**
     * Appends a child node to a parent node.
     *
     * @param parent parent node representation
     * @param child child node representation
     */
    protected abstract void appendChild(R parent, R child);

    /**
     * Materializes an {@link NodeTemplate.Element} template node.
     *
     * @param element element template node
     * @param execution active rendering execution context
     * @return materialized element node representation
     */
    protected abstract R materializeElement(NodeTemplate.Element element, RenderingExecution execution);

}