package org.jmouse.dom.renderer;

import org.jmouse.dom.Node;
import org.jmouse.dom.NodeType;
import org.jmouse.dom.TagName;

import java.util.Map;

/**
 * Renders {@link NodeType#ELEMENT} nodes into markup. 🧱
 *
 * <p>
 * Produces an opening tag with attributes, renders children (if present) via the provided
 * {@link RenderingProcessor}, and emits a closing tag when applicable.
 * </p>
 *
 * <h3>Behavior</h3>
 * <ul>
 *     <li>Tag name is lower-cased: {@code TagName.DIV → "div"}.</li>
 *     <li>Attributes are rendered as {@code key="value"} with escaping via
 *         {@link RenderSupport#escapeAttribute(String, RendererContext)}.</li>
 *     <li>Indentation/newlines are controlled by {@link RendererContext} and {@link RenderSupport}.</li>
 *     <li>Void tags are detected by {@link RenderSupport#isVoidTag(TagName, RendererContext)} and rendered
 *         without closing tag.</li>
 *     <li>Empty non-void elements:
 *         <ul>
 *             <li>in XML mode → {@code <tag />}</li>
 *             <li>otherwise → {@code <tag></tag>}</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * Renderer renderer = new ElementRenderer();
 * String html = renderer.render(elementNode, context, processor);
 * }</pre>
 */
public final class ElementRenderer implements Renderer {

    /**
     * Supports only {@link NodeType#ELEMENT} nodes.
     *
     * @param node node to check
     * @return {@code true} if node is non-null and element type
     */
    @Override
    public boolean supports(Node node) {
        return node != null && node.getNodeType() == NodeType.ELEMENT;
    }

    /**
     * Renders an element node.
     *
     * <p>
     * Children are rendered by delegating to {@link RenderingProcessor#renderInternal(Node, RendererContext)}
     * to ensure consistent renderer selection and recursion.
     * </p>
     *
     * @param node element node
     * @param context rendering context (indentation, escaping, newline policy)
     * @param processor rendering dispatcher used to render children
     * @return rendered markup string for the given node
     */
    @Override
    public String render(Node node, RendererContext context, RenderingProcessor processor) {
        TagName       tagName     = node.getTagName();
        String        tag         = tagName.name().toLowerCase();
        StringBuilder buffer      = new StringBuilder(128);
        boolean       voidTag     = RenderSupport.isVoidTag(tagName, context);
        boolean       hasChildren = node.hasChildren();

        buffer.append(RenderSupport.indent(node.getDepth(), context))
                .append('<')
                .append(tag);

        renderAttributes(buffer, node.getAttributes(), context);

        if (voidTag) {
            buffer.append('>')
                    .append(RenderSupport.newline(context));
            return buffer.toString();
        }

        if (!hasChildren) {
            if (context.escapeMode() == RendererContext.EscapeMode.XML) {
                buffer.append(" />").append(RenderSupport.newline(context));
            } else {
                buffer.append('>')
                        .append("</")
                        .append(tag)
                        .append('>')
                        .append(RenderSupport.newline(context));
            }
            return buffer.toString();
        }

        buffer.append('>').append(RenderSupport.newline(context));

        for (Node child : node.getChildren()) {
            buffer.append(processor.renderInternal(child, context));
        }

        buffer.append(RenderSupport.indent(node.getDepth(), context))
                .append("</")
                .append(tag)
                .append('>')
                .append(RenderSupport.newline(context));

        return buffer.toString();
    }

    /**
     * Renders element attributes into the buffer.
     *
     * <p>
     * Skips blank attribute names. Values are always escaped for attribute context.
     * </p>
     *
     * @param buffer output buffer
     * @param attributes attribute map (name → value)
     * @param context rendering context used for escaping rules
     */
    private void renderAttributes(StringBuilder buffer, Map<String, String> attributes, RendererContext context) {
        if (attributes == null || attributes.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String attribute = entry.getKey();
            String value     = entry.getValue();

            if (attribute == null || attribute.isBlank()) {
                continue;
            }

            buffer.append(' ')
                    .append(attribute)
                    .append("=\"")
                    .append(RenderSupport.escapeAttribute(value, context))
                    .append('"');
        }
    }
}