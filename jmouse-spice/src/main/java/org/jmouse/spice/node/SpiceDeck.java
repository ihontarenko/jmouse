package org.jmouse.spice.node;

import org.jmouse.el.node.Trivia;
import org.jmouse.el.writer.SourceWriter;
import org.jmouse.spice.SpicePosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A whole SPICE text: its title, then its statements — the deck's children — in order, up to {@code .end}.
 */
public final class SpiceDeck extends SpiceNode {

    private final String       title;
    private final List<Trivia> closingTrivia = new ArrayList<>();
    private int ignoredAfterEnd;

    /** @param title the first line, or {@code null} for a text without one — a library file */
    public SpiceDeck(String title, SpicePosition position) {
        super(position);
        this.title = title;
    }

    /** A deck a program builds: its title and its statements. */
    public static SpiceDeck of(String title, List<? extends SpiceStatement> statements) {
        SpiceDeck deck = new SpiceDeck(title, SpicePosition.NONE);
        statements.forEach(deck::add);
        return deck;
    }

    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /** The statements in order, subcircuits holding their own. */
    public List<SpiceStatement> statements() {
        return getChildren(SpiceStatement.class);
    }

    /** The comments after the last statement. */
    public List<Trivia> closingTrivia() {
        return List.copyOf(closingTrivia);
    }

    public void addClosingTrivia(List<Trivia> trivia) {
        closingTrivia.addAll(trivia);
    }

    /** How many cards follow {@code .end}, which SPICE does not read and neither does the parser. */
    public int ignoredAfterEnd() {
        return ignoredAfterEnd;
    }

    public void ignoredAfterEnd(int cards) {
        this.ignoredAfterEnd = cards;
    }

    /**
     * The title, the statements, the comments after them. SPICE reads a deck's first line as its title: a deck without
     * one whose first line is not a dot card gets an empty title line, or that line would be read as the title.
     */
    @Override
    public void writeSource(SourceWriter writer) {
        if (title != null) {
            if (title.isEmpty()) {
                writer.blankLine();
            } else {
                writer.word(title).endLine();
            }
        } else if (opensWithoutDotCard()) {
            writer.blankLine();
        }
        writer.each(statements());
        closingTrivia.forEach(trivia -> writeComment(writer, trivia));
    }

    /** Whether the deck's first line would be anything but a dot card: a comment, an element. */
    private boolean opensWithoutDotCard() {
        List<SpiceStatement> statements = statements();
        if (statements.isEmpty()) {
            return closingTrivia.stream().anyMatch(trivia -> !trivia.isBlank());
        }
        SpiceStatement first = statements.getFirst();
        return !first.getLeadingTrivia().isEmpty() || !(first instanceof SpiceCard);
    }

    @Override
    public <R> R accept(SpiceVisitor<R> visitor) {
        return visitor.visitDeck(this);
    }
}
