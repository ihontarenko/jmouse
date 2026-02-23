package org.jmouse.dom;

/**
 * {@link Corrector} that recalculates and normalizes node depth values. 📏
 *
 * <p>
 * For each visited node, depth is recomputed based on its parent:
 * </p>
 *
 * <ul>
 *     <li>If node has a parent → {@code parent.depth + 1}</li>
 *     <li>If node has no parent → {@code 0}</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 *
 * <p>
 * Useful after structural mutations such as:
 * </p>
 * <ul>
 *     <li>{@link Node#wrap(Node)}</li>
 *     <li>{@link Node#unwrap()}</li>
 *     <li>{@link Node#insertBefore(Node)}</li>
 *     <li>{@link Node#insertAfter(Node)}</li>
 * </ul>
 *
 * <pre>{@code
 * root.execute(new CorrectNodeDepth());
 * }</pre>
 *
 * <p>
 * ⚠ This corrector assumes the traversal order ensures that parent
 * nodes are processed before their children (which is true for
 * {@link Node#execute(java.util.function.Consumer)}).
 * </p>
 */
public class CorrectNodeDepth implements Corrector {

    /**
     * Recomputes depth for the given node.
     *
     * @param node node to correct
     */
    @Override
    public void accept(Node node) {
        node.setDepth(node.hasParent()
                ? node.getParent().getDepth() + 1
                : 0);
    }

}