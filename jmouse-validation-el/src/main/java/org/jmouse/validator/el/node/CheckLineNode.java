package org.jmouse.validator.el.node;

import org.jmouse.el.node.AbstractExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything asked of one field on one line — {@code part_number : required stop, size(3, 32)}.
 *
 * <p>Checks run left to right, which is why {@code stop} means anything at all: a check that silences
 * its followers needs an order to have followers in.</p>
 *
 * <p>⚠️ <strong>The line's own message is a continuation line, not an inline trailer.</strong> Written
 * inline it would be indistinguishable from a message on the last check, and a grammar where the same
 * characters mean two things is one nobody can read back. A {@code :} opening a line can only be a
 * continuation, because every other line opens with a field name.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class CheckLineNode extends AbstractExpression {

    private final List<CheckNode> checks = new ArrayList<>();

    private String field;
    private String message;
    private String checksNote;

    /** @return the field every check on this line is about */
    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    /** @return the checks, in the order they were written; never {@code null} */
    public List<CheckNode> getChecks() {
        return checks;
    }

    /**
     * Adds a check to the end of the line.
     *
     * @param check what to add
     */
    public void addCheck(CheckNode check) {
        checks.add(check);
        add(check);
    }

    /**
     * @return the message covering every check without one of its own, or {@code null}
     */
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * The comment written at the end of the <em>checks</em>, where a message continues below them.
     *
     * <h2>⚠️ A wrapped line has two ends, and a comment may sit on either</h2>
     *
     * <pre>{@code
     * part_number : required stop, notBlank, size(3, 32)   # the common failure
     *             : 'A part number looks like AB-1234'     # shown to the buyer
     * }</pre>
     *
     * <p>The second is the node's ordinary trailing trivia. The first has nowhere else to live: it
     * belongs to this line, not to the statement above or below it, and putting it on the node's single
     * trailing slot would print it under the message — moving somebody's aside a line down every time
     * the file is saved.</p>
     *
     * @return the comment, or {@code null}
     */
    public String getChecksNote() {
        return checksNote;
    }

    public void setChecksNote(String checksNote) {
        this.checksNote = checksNote;
    }

    @Override
    public String toString() {
        return field + " : " + checks;
    }
}
