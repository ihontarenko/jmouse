package org.jmouse.dom;

import org.jmouse.dom.node.CommentNode;

import java.util.function.Function;

/**
 * {@link Corrector} that injects diagnostic comments before and/or after each node. 🏷️
 *
 * <p>
 * For every visited node, this corrector may insert:
 * </p>
 * <ul>
 *     <li>a comment <b>before</b> the node</li>
 *     <li>a comment <b>after</b> the node</li>
 * </ul>
 *
 * <p>
 * The comment content is generated via user-provided functions.
 * </p>
 *
 * <h3>Default behavior</h3>
 *
 * <pre>{@code
 * OPEN:  [NODE_INFO]
 * CLOSE: [NODE_INFO]
 * }</pre>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * Corrector corrector = new CommentInfoCorrector(
 *     node -> "BEGIN: " + node,
 *     node -> "END: " + node
 * );
 *
 * root.execute(corrector);
 * }</pre>
 *
 * <p>
 * ⚠ This corrector performs structural mutation (inserts sibling nodes).
 * It should be used carefully during traversal to avoid unintended ordering effects.
 * </p>
 */
public class CommentInfoCorrector implements Corrector {

    private final Function<Node, String> beforeFunction;
    private final Function<Node, String> afterFunction;

    /**
     * Creates a corrector with default comment format:
     *
     * <pre>{@code
     * OPEN:  [node.toString()]
     * CLOSE: [node.toString()]
     * }</pre>
     */
    public CommentInfoCorrector() {
        this("OPEN: [%s]"::formatted, "CLOSE: [%s]"::formatted);
    }

    /**
     * Creates a corrector with custom comment generators.
     *
     * @param beforeFunction function generating comment inserted before node (nullable result)
     * @param afterFunction function generating comment inserted after node (nullable result)
     */
    public CommentInfoCorrector(
            Function<Node, String> beforeFunction,
            Function<Node, String> afterFunction
    ) {
        this.beforeFunction = beforeFunction;
        this.afterFunction = afterFunction;
    }

    /**
     * Inserts comments before and/or after the given node.
     *
     * <p>
     * If the generated comment text is {@code null},
     * no comment is inserted.
     * </p>
     *
     * @param node node being processed
     */
    @Override
    public void accept(Node node) {
        String before = beforeFunction.apply(node);

        if (before != null) {
            node.insertBefore(new CommentNode(before));
        }

        String after = afterFunction.apply(node);

        if (after != null) {
            node.insertAfter(new CommentNode(after));
        }
    }

}