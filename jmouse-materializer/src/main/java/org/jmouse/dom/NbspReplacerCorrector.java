package org.jmouse.dom;

import org.jmouse.dom.node.TextNode;

/**
 * {@link Corrector} implementation that replaces whitespace in text nodes
 * with {@code &nbsp;} entities. ␠➡️&nbsp;
 *
 * <p>
 * All consecutive whitespace characters ({@code \\s+}) inside
 * {@link TextNode} instances are replaced with the HTML non-breaking space entity.
 * </p>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * Node root = ...
 *
 * Corrector corrector = new NbspReplacerCorrector();
 * root.execute(corrector::accept);
 * }</pre>
 *
 * <p>
 * Input:
 * </p>
 * <pre>{@code
 * "Hello   world"
 * }</pre>
 *
 * <p>
 * Output:
 * </p>
 * <pre>{@code
 * "Hello&nbsp;world"
 * }</pre>
 *
 * <p>
 * ⚠ Note:
 * This transformation modifies text content directly and may interfere
 * with escaping rules depending on the active {@code RendererContext}.
 * It should typically be applied before rendering.
 * </p>
 */
public class NbspReplacerCorrector implements Corrector {

    /**
     * Replaces all whitespace sequences in {@link TextNode} instances
     * with {@code &nbsp;}.
     *
     * @param node node to inspect
     */
    @Override
    public void accept(Node node) {
        if (node instanceof TextNode textNode) {
            String value = textNode.getText();
            if (value != null) {
                textNode.setText(value.replaceAll("\\s+", "&nbsp;"));
            }
        }
    }

}