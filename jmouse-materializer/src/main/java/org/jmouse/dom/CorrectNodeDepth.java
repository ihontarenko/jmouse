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
 */
public class CorrectNodeDepth implements Corrector {

    /**
     * Recomputes depth for the given node.
     *
     * @param node node to correct
     */
    @Override
    public void accept(Node node) {
        node.setDepth(
                node.hasParent() ? node.getParent().getDepth() + 1 : 0
        );
    }

}