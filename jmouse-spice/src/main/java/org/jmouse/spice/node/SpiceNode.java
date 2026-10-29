package org.jmouse.spice.node;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Trivia;
import org.jmouse.el.writer.SourceLayout;
import org.jmouse.el.writer.SourceWriter;
import org.jmouse.el.writer.TextSourceWriter;
import org.jmouse.spice.SpicePosition;

/**
 * A node of a SPICE syntax tree: a jME expression, so a deck is walked, its comments carried as
 * {@link org.jmouse.el.node.Trivia} and its children reached the way any jME tree is — and, for SPICE's own nodes, a
 * {@link SpiceVisitor} that sees each kind by its type.
 *
 * <p>Every node writes itself into jME's {@link SourceWriter}: values, expressions and words as they were read —
 * {@code 10uF} stays {@code 10uF}, {@code {2*R1}} stays in its braces —, a card a line, continuation lines joined, its
 * comments where the tree keeps them. {@link #toSource()} is that, a blank between words.</p>
 *
 * <p>A comment line before a card is that card's leading trivia; a comment after a card on its line is its trailing
 * trivia; comments after the last card of a deck or a subcircuit are the deck's or the subcircuit's closing trivia.</p>
 */
public abstract sealed class SpiceNode extends AbstractExpression permits SpiceDeck, SpiceStatement {

    /** How SPICE is laid out: words a blank apart, a text in double quotes. */
    public static final SourceLayout LAYOUT = SourceLayout.WORDS;

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

    @Override
    public abstract void writeSource(SourceWriter writer);

    @Override
    public final String toSource() {
        return TextSourceWriter.render(this, LAYOUT);
    }

    /** A comment line as written; one a program wrote without SPICE's mark gets {@code *} in front. */
    static void writeComment(SourceWriter writer, Trivia trivia) {
        if (trivia.isBlank()) {
            writer.blankLine();
            return;
        }
        String text = trivia.text();
        if (!(text.startsWith("*") || text.startsWith(";") || text.startsWith("$"))) {
            writer.word("*");
        }
        writer.word(text).endLine();
    }
}
