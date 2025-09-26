package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.NodeVisitor;

/**
 * Represents a "for" loop node in the view engine.
 * <p>
 * A ForNode encapsulates the loop variable (item), an iterable expression,
 * the body (content to render for each item), and an optional empty block to be rendered
 * when the iterable is empty.
 * </p>
 */
public class ForNode extends AbstractNode {

    /**
     * The name of the loop variable used in the for-each construct.
     */
    private String item;

    /**
     * The expression that evaluates to an iterable collection.
     */
    private Expression iterable;

    /**
     * The body of the loop, rendered for each element in the iterable.
     */
    private Node body;

    /**
     * The optional block rendered if the iterable is empty.
     */
    private Node empty;

    /**
     * Returns the name of the loop variable.
     *
     * @return the loop variable name
     */
    public String getItem() {
        return item;
    }

    /**
     * Sets the loop variable name.
     *
     * @param item the loop variable name to set
     */
    public void setItem(String item) {
        this.item = item;
    }

    /**
     * Returns the iterable expression.
     *
     * @return the expression that evaluates to the iterable
     */
    public Expression getIterable() {
        return iterable;
    }

    /**
     * Sets the iterable expression.
     *
     * @param iterable the expression to set as the iterable
     */
    public void setIterable(Expression iterable) {
        this.iterable = iterable;
    }

    /**
     * Returns the body node of the loop.
     *
     * @return the node representing the loop body
     */
    public Node getBody() {
        return body;
    }

    /**
     * Sets the body node of the loop.
     *
     * @param body the node to set as the loop body
     */
    public void setBody(Node body) {
        this.body = body;
    }

    /**
     * Returns the optional empty block node.
     *
     * @return the node representing the block to render if the iterable is empty,
     *         or {@code null} if not defined
     */
    public Node getEmpty() {
        return empty;
    }

    /**
     * Sets the optional empty block node.
     *
     * @param empty the node to set as the empty block
     */
    public void setEmpty(Node empty) {
        this.empty = empty;
    }

    /**
     * Accepts the given visitor for processing this node.
     * <p>
     * If the visitor implements {@link NodeVisitor}, the specialized {@code visit(ForNode)}
     * method is invoked.
     * </p>
     *
     * @param visitor the visitor that processes this node
     */
    @Override
    public void accept(Visitor visitor) {
        if (visitor instanceof NodeVisitor nv) {
            nv.visit(this);
        }
    }

    /**
     * Returns a string representation of the ForNode.
     * <p>
     * Typically, it returns "FOR: " followed by the string representation of the iterable expression.
     * </p>
     *
     * @return a string representation of the for loop node
     */
    @Override
    public String toString() {
        return "FOR: " + getIterable();
    }
}
