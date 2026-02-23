package org.jmouse.dom.renderer;

import org.jmouse.dom.Node;
import org.jmouse.dom.node.CDataNode;

/**
 * Renderer for {@link CDataNode} instances. 📦
 *
 * <p>
 * Produces a CDATA section in the form:
 * </p>
 *
 * <pre>{@code
 * <![CDATA[ ... ]]>
 * }</pre>
 *
 * <h3>Safety handling</h3>
 * <p>
 * If the text contains the sequence {@code "]]>"}, it is split safely into
 * multiple CDATA sections to avoid prematurely terminating the block:
 * </p>
 *
 * <pre>{@code
 * "]]>" → "]]]]><![CDATA[>"
 * }</pre>
 *
 * <p>
 * Indentation and newline handling respect {@link RendererContext}.
 * </p>
 */
public final class CDataRenderer implements Renderer {

    /**
     * Supports {@link CDataNode} only.
     *
     * @param node node to inspect
     * @return {@code true} if node is a {@link CDataNode}
     */
    @Override
    public boolean supports(Node node) {
        return node instanceof CDataNode;
    }

    /**
     * Renders the CDATA node.
     *
     * <p>
     * Null text values are treated as empty strings.
     * The CDATA content is not escaped — it is emitted as-is,
     * except for safe splitting of the {@code "]]>"} sequence.
     * </p>
     *
     * @param node CDATA node
     * @param context renderer context (indentation/newline policy)
     * @param processor rendering dispatcher (not used here)
     * @return rendered CDATA section
     */
    @Override
    public String render(Node node, RendererContext context, RenderingProcessor processor) {
        CDataNode cdata     = (CDataNode) node;
        String    value     = cdata.getText() == null ? "" : cdata.getText();
        String    safeValue = value.replace("]]>", "]]]]><![CDATA[>");

        return RenderSupport.indent(node.getDepth(), context)
                + "<![CDATA[" + safeValue + "]]>"
                + RenderSupport.newline(context);
    }
}