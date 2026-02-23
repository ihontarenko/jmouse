package org.jmouse.dom;

import java.util.function.Consumer;

/**
 * Functional interface for DOM tree correction or normalization. 🛠️
 *
 * <p>
 * A {@code Corrector} is typically used to traverse and modify
 * a {@link Node} tree before rendering or further processing.
 * </p>
 *
 * <p>
 * Since it extends {@link Consumer}&lt;{@link Node}&gt;,
 * it can be used directly with {@link Node#execute(java.util.function.Consumer)}.
 * </p>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * Corrector corrector = new NbspReplacerCorrector();
 *
 * root.execute(corrector); // depth-first correction
 * }</pre>
 *
 * <p>
 * Implementations should be idempotent where possible and avoid
 * structural mutations that may break traversal.
 * </p>
 */
public interface Corrector extends Consumer<Node> {

    /**
     * Applies correction to the given node.
     *
     * <p>
     * Default implementation delegates to {@link #correct(Node)} (Node)}.
     * </p>
     *
     * @param node node to correct
     */
    default void correct(Node node) {
        accept(node);
    }

}