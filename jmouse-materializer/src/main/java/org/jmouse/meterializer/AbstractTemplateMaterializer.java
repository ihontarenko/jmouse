package org.jmouse.meterializer;

import org.jmouse.core.Verify;
import org.jmouse.core.access.ObjectAccessor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jmouse.util.Strings.isNotEmpty;

/**
 * Base implementation of {@link TemplateMaterializer} with built-in traversal logic. 🧱
 *
 * <p>
 * This class implements a generic materialization algorithm for {@link NodeTemplate}
 * and delegates concrete node creation to subclass hooks.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *     <li>Resolves {@link ValueExpression} via provided {@link ValueResolver}</li>
 *     <li>Evaluates {@link TemplatePredicate} via {@link PredicateEvaluator}</li>
 *     <li>Traverses blueprint nodes (text, conditional, repeat, include)</li>
 *     <li>Delegates element materialization and node construction to subclass</li>
 * </ul>
 *
 * <h3>Extending</h3>
 * <p>
 * Subclasses implement the target representation {@code R} (e.g. DOM node, HTML builder, custom UI model)
 * by providing implementations for:
 * </p>
 * <ul>
 *     <li>{@link #createElementNode(String)}</li>
 *     <li>{@link #createTextNode(String)}</li>
 *     <li>{@link #createContainerNode()}</li>
 *     <li>{@link #emptyNode()}</li>
 *     <li>{@link #appendChild(Object, Object)}</li>
 *     <li>{@link #materializeElement(NodeTemplate.Element, RenderingExecution)}</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * final class HtmlStringMaterializer extends AbstractTemplateMaterializer<StringBuilder> {
 *
 *     HtmlStringMaterializer(ValueResolver resolver) {
 *         super(resolver);
 *     }
 *
 *     @Override protected StringBuilder createElementNode(String tagName) { ... }
 *     @Override protected StringBuilder createTextNode(String text) { ... }
 *     @Override protected StringBuilder createContainerNode() { ... }
 *     @Override protected StringBuilder emptyNode() { return new StringBuilder(); }
 *     @Override protected void appendChild(StringBuilder parent, StringBuilder child) { ... }
 *     @Override protected StringBuilder materializeElement(NodeTemplate.Element element, RenderingExecution execution) { ... }
 * }
 * }</pre>
 *
 * <p>
 * The materializer itself is mostly stateless; thread-safety depends on the concrete {@code R}
 * type and the provided {@link ValueResolver}/{@link NodeTemplateResolver} implementations.
 * </p>
 *
 * @param <R> resulting node type produced by this materializer
 */
public abstract class AbstractTemplateMaterializer<R> implements TemplateMaterializer<R> {

    /**
     * Resolves runtime values from {@link ValueExpression}s. 🧩
     */
    protected final ValueResolver      valueResolver;

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
        this.valueResolver = Verify.nonNull(valueResolver, "valueResolver");
        this.predicateEvaluator = new PredicateEvaluator(this.valueResolver);
    }

    /**
     * Materializes the given blueprint node.
     *
     * <p>
     * This method performs basic argument validation and delegates to
     * {@link #materializeInternal(NodeTemplate, RenderingExecution)}.
     * </p>
     *
     * @param template blueprint node
     * @param execution active rendering execution context
     * @return materialized result (may be {@code null} depending on node type)
     */
    @Override
    public final R materialize(NodeTemplate template, RenderingExecution execution) {
        Verify.nonNull(template, "template");
        Verify.nonNull(execution, "execution");
        return materializeInternal(template, execution);
    }

    /**
     * Dispatches materialization to node-specific handlers.
     *
     * @param template blueprint node
     * @param execution active rendering execution context
     * @return materialized node or {@code null}
     */
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

    /**
     * Materializes a text node by resolving its value and converting it to a string.
     *
     * <p>
     * {@code null} resolved values are converted to empty string.
     * </p>
     *
     * @param text text blueprint node
     * @param execution active rendering execution context
     * @return created text node
     */
    protected R materializeText(NodeTemplate.Text text, RenderingExecution execution) {
        Object value = valueResolver.resolve(text.value(), execution);
        return createTextNode(value == null ? "" : String.valueOf(value));
    }

    /**
     * Materializes a conditional node by evaluating its predicate and picking a branch.
     *
     * <p>
     * If the selected branch is empty, {@link #emptyNode()} is returned.
     * If the branch contains exactly one node, it is materialized directly.
     * Otherwise, a container is created and all child nodes are appended.
     * </p>
     *
     * @param conditional conditional blueprint node
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
     * Materializes a repeat node by iterating over a resolved collection-like value. 🔁
     *
     * <p>
     * The collection is wrapped into an {@link ObjectAccessor} to support lists/maps/collections uniformly.
     * For each entry, the current item is bound into {@link RenderingExecution#variables()}
     * under {@link NodeTemplate.Repeat#itemVariableName()} and the body is materialized.
     * </p>
     *
     * <p>
     * If the resolved value is not a supported container type, {@link #emptyNode()} is returned.
     * </p>
     *
     * @param repeat repeat blueprint node
     * @param execution active rendering execution context
     * @return materialized container node
     */
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

    /**
     * Materializes an include node by resolving a referenced blueprint and materializing it
     * under a nested execution context. 🧬
     *
     * <p>
     * Behavior:
     * </p>
     * <ul>
     *     <li>resolve {@code templateReference}; if {@code null} → {@link #emptyNode()}</li>
     *     <li>resolve {@code model} and wrap as new root accessor</li>
     *     <li>create a nested {@link RenderingExecution}</li>
     *     <li>copy parent variables into nested execution</li>
     *     <li>resolve included blueprint via {@link NodeTemplateResolver} and materialize it</li>
     * </ul>
     *
     * @param include include blueprint node
     * @param execution active rendering execution context
     * @return materialized included subtree
     */
    protected R materializeInclude(NodeTemplate.Include include, RenderingExecution execution) {
        Object keyValue = valueResolver.resolve(include.templateReference(), execution);

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
     * <p>
     * Used for non-rendering results (empty branches, invalid repeat inputs, etc.).
     * </p>
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
     * Materializes an {@link NodeTemplate.Element} node.
     *
     * <p>
     * Subclasses usually:
     * </p>
     * <ul>
     *     <li>create element node</li>
     *     <li>resolve/apply attributes</li>
     *     <li>materialize children</li>
     *     <li>apply directives (if supported)</li>
     * </ul>
     *
     * @param element element blueprint node
     * @param execution active rendering execution context
     * @return materialized element node representation
     */
    protected abstract R materializeElement(NodeTemplate.Element element, RenderingExecution execution);

}