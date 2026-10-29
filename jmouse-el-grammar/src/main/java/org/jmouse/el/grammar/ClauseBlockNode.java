package org.jmouse.el.grammar;

import org.jmouse.el.writer.SourceWriter;

import java.util.Optional;

/**
 * A block read by a {@link ClauseBlockParser}: its word, what its head says — {@code "title" from "source"},
 * {@code uses one, other}, {@code transient for 80 ms} —, and its statements, when it is opened with braces.
 */
public class ClauseBlockNode extends BlockNode {

    private final String         word;
    private final ClauseLineNode header;
    private boolean opened;

    /** @param header what its head says, however it was made — a block writes its word itself, so its head never does */
    public ClauseBlockNode(String word, ClauseLineNode header) {
        this.word = word;
        this.header = header == null ? null : header.asHeader();
    }

    @Override
    public String word() {
        return word;
    }

    /** What stands after the word, before the braces; empty for a block whose head says nothing. */
    public Optional<ClauseLineNode> header() {
        return Optional.ofNullable(header);
    }

    /** Whether the block has a body in braces, rather than its head alone. */
    public boolean opened() {
        return opened;
    }

    /** Says the block has a body in braces: a reader that gathers terms into it opens it. */
    public void open() {
        opened = true;
    }

    @Override
    public void writeSource(SourceWriter writer) {
        if (header != null) {
            header.prefixes().forEach(writer::word);
        }
        writer.word(word);
        if (header != null) {
            writer.write(header);
        }
        if (opened) {
            writer.block(body -> StatementWriting.statements(body, getExpressions()));
        }
    }
}
