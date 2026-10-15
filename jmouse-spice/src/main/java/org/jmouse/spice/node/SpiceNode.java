package org.jmouse.spice.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.spice.SpicePosition;

/**
 * A node of a SPICE syntax tree: a jME {@link org.jmouse.el.node.Node}, so a deck is walked, its comments carried as
 * {@link org.jmouse.el.node.Trivia} and its children reached the way any jME tree is — and, for SPICE's own nodes, a
 * {@link SpiceVisitor} that sees each kind by its type.
 *
 * <p>A comment line before a card is that card's leading trivia; a comment after a card on its line is its trailing
 * trivia; comments after the last card of a deck or a subcircuit are the deck's or the subcircuit's closing trivia.</p>
 */
public abstract sealed class SpiceNode extends AbstractNode permits SpiceDeck, SpiceStatement {

    private final SpicePosition position;

    protected SpiceNode(SpicePosition position) {
        this.position = position == null ? SpicePosition.NONE : position;
    }

    /** Where the node begins in the text it was read from; {@link SpicePosition#NONE} for one a program built. */
    public SpicePosition position() {
        return position;
    }

    /** Hands the node to the visitor's method for its kind. */
    public abstract <R> R accept(SpiceVisitor<R> visitor);
}
