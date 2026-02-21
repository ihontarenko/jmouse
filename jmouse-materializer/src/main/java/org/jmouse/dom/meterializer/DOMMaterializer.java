package org.jmouse.dom.meterializer;

import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;
import org.jmouse.dom.node.ElementNode;
import org.jmouse.dom.node.TextNode;
import org.jmouse.meterializer.*;

import java.util.List;
import java.util.Map;

import static org.jmouse.core.Verify.nonNull;

/**
 * {@link TemplateMaterializer} implementation that produces jMouse DOM {@link Node}s. 🌳
 *
 * <p>
 * This materializer converts {@link NodeTemplate} blueprints into concrete DOM nodes:
 * {@link ElementNode} for elements and {@link TextNode} for text.
 * It inherits the traversal logic (conditional, repeat, include) from
 * {@link AbstractTemplateMaterializer} and provides DOM-specific node construction.
 * </p>
 *
 * <h3>What it does</h3>
 * <ul>
 *     <li>creates DOM elements/text nodes</li>
 *     <li>applies element attributes resolved via {@link ValueResolver}</li>
 *     <li>applies {@link NodeDirective}s (omit, wrap, attribute/class mutations)</li>
 *     <li>materializes children recursively</li>
 * </ul>
 *
 * <h3>Default configuration</h3>
 * <p>
 * The no-args constructor uses:
 * </p>
 * <ul>
 *     <li>{@link DefaultValueResolver}</li>
 *     <li>{@link PathValueResolver}</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * DOMMaterializer materializer = new DOMMaterializer();
 *
 * NodeTemplate template = NodeTemplate.element("div", div -> div
 *     .attribute("class", ValueExpression.constant("card"))
 *     .children(children -> children
 *         .text("Hello ")
 *         .text(ValueExpression.path("user.name"))
 *     )
 * );
 *
 * Node dom = materializer.materialize(template, execution);
 * }</pre>
 *
 * <p>
 * Note: {@link #toTagName(String)} maps names to {@link TagName} via {@code valueOf(...)}.
 * This implies tag names must exist in {@link TagName} enum.
 * </p>
 */
public class DOMMaterializer extends AbstractTemplateMaterializer<Node> {

    /**
     * Creates a DOM materializer using the default resolver stack.
     *
     * <p>
     * Equivalent to {@code new DOMMaterializer(new DefaultValueResolver(new PathValueResolver()))}.
     * </p>
     */
    public DOMMaterializer() {
        super(new ConfigurableValueResolver(ResolutionMode.FULL, new PathValueResolver()));
    }

    /**
     * Creates a DOM materializer with a custom {@link ValueResolver}.
     *
     * @param valueResolver resolver used for resolving {@link ValueExpression}s
     */
    public DOMMaterializer(ValueResolver valueResolver) {
        super(nonNull(valueResolver, "valueResolver"));
    }

    /**
     * Creates a DOM {@link ElementNode} for the given tag name.
     *
     * @param tagName element tag name
     * @return element node
     */
    @Override
    protected Node createElementNode(String tagName) {
        return new ElementNode(toTagName(nonNull(tagName, "qName")));
    }

    /**
     * Creates a DOM {@link TextNode} from the given text.
     *
     * @param text text content (nullable; treated as empty string)
     * @return text node
     */
    @Override
    protected Node createTextNode(String text) {
        return new TextNode(text == null ? "" : text);
    }

    /**
     * Creates a container node used for multi-node branches.
     *
     * <p>
     * The current implementation uses a {@code <div>} element.
     * </p>
     *
     * @return container node
     */
    @Override
    protected Node createContainerNode() {
        return new ElementNode(TagName.DIV);
    }

    /**
     * Returns an empty DOM node representation.
     *
     * <p>
     * Used for empty branches and other non-rendering outcomes.
     * </p>
     *
     * @return empty text node
     */
    @Override
    protected Node emptyNode() {
        return new TextNode("");
    }

    /**
     * Appends {@code child} node to {@code parent}.
     *
     * <p>
     * Only {@link ElementNode} can accept children. Attempting to append to a
     * non-element node results in {@link IllegalStateException}.
     * </p>
     *
     * @param parent parent node (must be {@link ElementNode})
     * @param child child node
     */
    @Override
    protected void appendChild(Node parent, Node child) {
        nonNull(parent, "parent");
        nonNull(child, "child");

        if (!(parent instanceof ElementNode element)) {
            throw new IllegalStateException("Cannot append child to non-element node: " + parent.getClass().getName());
        }

        element.append(child);
    }

    /**
     * Materializes an {@link NodeTemplate.Element} into a DOM {@link ElementNode}.
     *
     * <p>
     * Steps:
     * </p>
     * <ol>
     *     <li>create element node</li>
     *     <li>apply resolved attributes</li>
     *     <li>apply directives (may omit/wrap/mutate node)</li>
     *     <li>materialize children into the effective content node</li>
     * </ol>
     *
     * <p>
     * If directives produce {@code omitted} outcome, {@code null} is returned and
     * the caller should skip appending this node.
     * </p>
     *
     * @param element element blueprint node
     * @param execution active rendering execution context
     * @return rendered DOM node or {@code null} if omitted by directive
     */
    @Override
    protected Node materializeElement(NodeTemplate.Element element, RenderingExecution execution) {
        nonNull(element, "element");
        nonNull(execution, "execution");

        ElementNode node = new ElementNode(toTagName(element.tagName()));

        applyAttributes(node, element.attributes(), execution);

        DirectiveOutcome<Node> outcome = applyDirectives(node, element.directives(), execution);

        if (outcome.isOmitted()) {
            return null;
        }

        ElementNode contentNode = (ElementNode) outcome.node();

        for (NodeTemplate child : element.children()) {
            Node childNode = materializeInternal(child, execution);
            if (childNode != null) {
                contentNode.append(childNode);
            }
        }

        return outcome.root();
    }

    /**
     * Applies resolved attributes to a DOM element. 🏷️
     *
     * <p>
     * Rules:
     * </p>
     * <ul>
     *     <li>skip null/empty attribute maps</li>
     *     <li>skip blank attribute names</li>
     *     <li>skip null expressions</li>
     *     <li>skip null resolved values</li>
     *     <li>values are stringified via {@link String#valueOf(Object)}</li>
     * </ul>
     *
     * @param node target DOM element node
     * @param attributes blueprint attributes
     * @param execution active rendering execution context
     */
    private void applyAttributes(
            ElementNode node,
            Map<String, ValueExpression> attributes,
            RenderingExecution execution
    ) {
        if (attributes == null || attributes.isEmpty()) {
            return;
        }

        for (Map.Entry<String, ValueExpression> entry : attributes.entrySet()) {
            String name = entry.getKey();
            ValueExpression expression = entry.getValue();

            if (name == null || name.isBlank()) {
                continue;
            }
            if (expression == null) {
                continue;
            }

            Object value = valueResolver.resolve(expression, execution);

            if (value == null) {
                continue;
            }

            node.addAttribute(name, String.valueOf(value));
        }
    }

    /**
     * Applies {@link NodeDirective}s to an element node. 🧰
     *
     * <p>
     * Supported directives (behavior depends on predicate evaluation and resolver):
     * </p>
     * <ul>
     *     <li>{@link NodeDirective.OmitIf} — omit node completely</li>
     *     <li>{@link NodeDirective.SetAttributeIf} — conditionally set attribute</li>
     *     <li>{@link NodeDirective.RemoveAttributeIf} — conditionally remove attribute</li>
     *     <li>{@link NodeDirective.AddClassIf} — conditionally add CSS classes</li>
     *     <li>{@link NodeDirective.WrapIf} — conditionally wrap the node</li>
     * </ul>
     *
     * <p>
     * Wrapping may change the returned root: the original {@code node} becomes nested,
     * while {@code root} points to the outer wrapper.
     * </p>
     *
     * @param node element node to mutate
     * @param directives directive list (nullable/empty means no-op)
     * @param execution active rendering execution context
     * @return directive outcome describing whether node is kept/omitted/wrapped
     */
    private DirectiveOutcome<Node> applyDirectives(
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
                            String classNames = String.valueOf(value).trim();
                            if (!classNames.isEmpty()) {
                                Node.addClass(node, classNames);
                            }
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
                case null -> { }
            }
        }

        return DirectiveOutcome.wrapped(node, root);
    }

    /**
     * Converts a raw tag name string into {@link TagName}. 🔤
     *
     * <p>
     * Normalizes input by trimming and converting to upper case,
     * then calls {@link TagName#valueOf(String)}.
     * </p>
     *
     * @param tagName raw tag name
     * @return resolved {@link TagName}
     *
     * @throws IllegalArgumentException if tag name is blank or not present in {@link TagName}
     */
    private TagName toTagName(String tagName) {
        String normalized = nonNull(tagName, "qName").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("qName is blank");
        }
        return TagName.valueOf(normalized.toUpperCase());
    }

}