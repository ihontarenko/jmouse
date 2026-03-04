package org.jmouse.dom.meterializer;

import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;
import org.jmouse.dom.node.ElementNode;
import org.jmouse.dom.node.TextNode;
import org.jmouse.dom.node.WrapperNode;
import org.jmouse.meterializer.*;
import org.jmouse.util.Strings;

import static org.jmouse.core.Verify.nonNull;

/**
 * DOM-oriented {@link TemplateMaterializer} that turns {@link NodeTemplate}s into jMouse {@link Node}s.
 *
 * <p>
 * Produces {@link ElementNode}/{@link TextNode} and uses {@link WrapperNode} as a temporary container
 * for multi-node branches (e.g. directive-expanded output), flattening it into the parent.
 * </p>
 */
public class DOMMaterializer extends AbstractTemplateMaterializer<Node> {

    /**
     * Creates a materializer with a default resolver stack.
     *
     * <p>
     * Uses {@link ConfigurableValueResolver} in {@link ResolutionMode#FULL} mode with {@link PathValueResolver}.
     * </p>
     */
    public DOMMaterializer() {
        super(new ConfigurableValueResolver(ResolutionMode.FULL, new PathValueResolver()));
    }

    /**
     * Creates a materializer with a custom {@link ValueResolver}.
     *
     * @param valueResolver resolver used to evaluate {@link ValueExpression}s
     */
    public DOMMaterializer(ValueResolver valueResolver) {
        super(nonNull(valueResolver, "valueResolver"));
    }

    /**
     * Creates a DOM element node for the given tag name.
     *
     * @param tagName element tag name (non-blank)
     * @return created element node
     */
    @Override
    protected Node createElementNode(String tagName) {
        return new ElementNode(toTagName(nonNull(tagName, "qName")));
    }

    /**
     * Creates a text node for the given text.
     *
     * @param text text content (nullable; treated as empty)
     * @return created text node
     */
    @Override
    protected Node createTextNode(String text) {
        return new TextNode(text == null ? "" : text);
    }

    /**
     * Creates a container node for multi-node branches.
     *
     * <p>
     * Used as an internal wrapper so branches can return multiple nodes and still be appended as a unit.
     * </p>
     *
     * @return wrapper container node
     */
    @Override
    protected Node createContainerNode() {
        return new WrapperNode();
    }

    /**
     * Returns an "empty" node representation.
     *
     * <p>
     * Used for empty branches and non-rendering outcomes.
     * </p>
     *
     * @return empty text node
     */
    @Override
    protected Node emptyNode() {
        return new TextNode("");
    }

    /**
     * Appends a child to a parent node.
     *
     * <p>
     * Only {@link ElementNode} can accept children.
     * </p>
     *
     * @param parent parent node
     * @param child  child node
     * @throws IllegalStateException if {@code parent} is not an {@link ElementNode}
     */
    @Override
    protected void appendChild(Node parent, Node child) {
        nonNull(parent, "parent").append(nonNull(child, "child"));
    }

    /**
     * Materializes an {@link NodeTemplate.Element} into an {@link ElementNode}.
     *
     * <p>
     * Applies attributes and directives; if directives omit the node, returns {@code null}.
     * If a child materializes into {@link WrapperNode}, its children are flattened into the content node.
     * </p>
     *
     * @param element    element template
     * @param execution  current rendering execution
     * @return root node produced by directives, or {@code null} if omitted
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

        for (NodeTemplate childTemplate : element.children()) {
            Node childNode = materializeInternal(childTemplate, execution);
            if (childNode != null) {
                if (childNode instanceof WrapperNode wrapper) {
                    for (Node inner : wrapper.getChildren()) {
                        contentNode.append(inner);
                    }
                } else {
                    contentNode.append(childNode);
                }
            }
        }

        return outcome.root();
    }

    /**
     * Sets an attribute on the given node.
     *
     * @param node  target node
     * @param name  attribute name
     * @param value attribute value
     */
    @Override
    protected void setAttribute(Node node, String name, String value) {
        node.addAttribute(name, value);
    }

    /**
     * Removes an attribute from the given node.
     *
     * @param node target node
     * @param name attribute name
     */
    @Override
    protected void removeAttribute(Node node, String name) {
        node.getAttributes().remove(name);
    }

    /**
     * Adds one or more CSS class names to the node.
     *
     * @param node       target node
     * @param classNames space-separated classes
     */
    @Override
    protected void addClass(Node node, String classNames) {
        Node.addClass(node, classNames);
    }

    /**
     * Wraps a node into a newly created element wrapper.
     *
     * @param node           target node to wrap
     * @param wrapTo wrapper tag name (non-blank)
     * @return wrapper element
     */
    @Override
    protected Node wrapElement(Node node, String wrapTo) {
        ElementNode wrapper = new ElementNode(toTagName(wrapTo));
        node.wrap(wrapper);
        return wrapper;
    }

    /**
     * Normalizes and converts a string tag into {@link TagName}.
     *
     * @param tagName raw tag name
     * @return enum tag name
     * @throws IllegalArgumentException if blank or unknown
     */
    private TagName toTagName(String tagName) {
        String normalized = Strings.normalize(tagName, String::trim);

        if (Strings.isEmpty(normalized)) {
            throw new IllegalArgumentException("qName is blank");
        }

        return TagName.valueOf(normalized.toUpperCase());
    }

}