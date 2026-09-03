package org.jmouse.validator.el.node;

import org.jmouse.el.node.expression.ExpressionsNode;

/**
 * A {@code gate { … }} or an {@code always { … }}.
 *
 * <p>One class for two words because they differ in exactly one property — what a failure inside them
 * means — and nothing else. Two classes would duplicate a body and a walk to record a boolean.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class CheckBlockNode extends ExpressionsNode {

    private Kind kind;

    /** Which of the two blocks this is. */
    public enum Kind {

        /**
         * ⚠️ Runs first, and a failure inside it answers for the whole document.
         *
         * <p>Not "the most important checks". A record of the wrong shape is not a record with several
         * faults — it is a record nothing else in the document is about, so continuing would report
         * faults against a shape nobody claimed it had.</p>
         */
        GATE,

        /** The unguarded block: what holds whatever the record looks like. */
        ALWAYS
    }

    /** @return whether this is the gate or the unguarded block */
    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    @Override
    public String toString() {
        return (kind == null ? "block" : kind.name().toLowerCase()) + " { … }";
    }
}
