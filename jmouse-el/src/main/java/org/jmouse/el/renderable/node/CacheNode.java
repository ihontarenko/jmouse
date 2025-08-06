package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.NodeVisitor;

/**
 * Represents a cache directive in a view.
 * <p>
 * The {@code CacheNode} captures a block of content and associates it with a cache key.
 * When rendering, the engine may store or retrieve the rendered output for that key.
 * </p>
 * <p>
 * Syntax example:
 * <pre>
 *   {% cache user.id %} ... {% endcache %}
 * </pre>
 * </p>
 */
public class CacheNode extends AbstractNode {

    /** The expression whose evaluated value is used as the cache key. */
    private ExpressionNode key;

    /** The block of content to be cached under the computed key. */
    private Node content;

    /**
     * Returns the expression used to compute the cache key.
     *
     * @return the cache key expression
     */
    public ExpressionNode getKey() {
        return key;
    }

    /**
     * Sets the expression used to compute the cache key.
     *
     * @param key the cache key expression to set
     */
    public void setKey(ExpressionNode key) {
        this.key = key;
    }

    /**
     * Returns the content node whose output will be cached.
     *
     * @return the content node
     */
    public Node getContent() {
        return content;
    }

    /**
     * Sets the content node whose output should be cached.
     *
     * @param content the node representing the content block to cache
     */
    public void setContent(Node content) {
        this.content = content;
    }

    /**
     * Accepts a visitor to process this cache node.
     * <p>
     * Delegates to {@link NodeVisitor#visit(CacheNode)} if the visitor is a {@link NodeVisitor}.
     * </p>
     *
     * @param visitor the visitor to process this node
     */
    @Override
    public void accept(Visitor visitor) {
        if (visitor instanceof NodeVisitor nv) {
            nv.visit(this);
        }
    }

    /**
     * Returns a string representation of this node for debugging.
     *
     * @return a string in the form "CACHE: &lt;key&gt;"
     */
    @Override
    public String toString() {
        return "CACHE: %s".formatted(key);
    }
}
