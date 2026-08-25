package org.jmouse.query.el.node;

import org.jmouse.query.translate.Capability;

/**
 * What a clause <em>is</em> — the four things everything outside the parser needs to know about one.
 *
 * <h2>⚠️ Carried by the clause, not held in a table somewhere</h2>
 *
 * <p>Three places used to hold their own list of the five clauses that existed: a canonical order for
 * un-parsing, a chain of {@code if} statements deciding which capability each one needs, and a rule
 * about repeats. Adding a sixth clause meant finding all three — and forgetting the first was the worst
 * of them, because a clause missing from the rendering order <strong>parses and then vanishes on the way
 * back out</strong>. A saved query quietly losing a clause when it is rewritten is the failure this type
 * exists to make impossible.</p>
 *
 * <p>So a clause states its own order, its own capability and its own repeatability, and nothing has a
 * list to keep in step.</p>
 *
 * @param keyword    the word that opens it — {@code where}, {@code order}
 * @param capability what a backend must declare in order to honour it
 * @param order      where it sits when the block is written back out; lower comes first
 * @param repetition how often it may be written, and what a repeat means
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record ClauseKind(String keyword, Capability capability, int order, Repetition repetition) {

    /**
     * How often a clause may be written, and what a repeat MEANS.
     *
     * <h2>⚠️ Three states, because two were not enough</h2>
     *
     * <p>A boolean said "repeatable" and meant "combined into one". That is right for {@code where}, whose
     * repeats are an {@code and}, and wrong for {@code join}: two joins are two tables and there is no one
     * clause that says both. With only two states a second join was refused, and the advice it gave —
     * <em>put everything it says on the one line</em> — was impossible to follow.</p>
     */
    public enum Repetition {

        /** Written once. A second is a mistake, and everything it says fits on the one line. */
        ONCE,

        /** Written several times and COMBINED — {@code where} twice is an {@code and}. */
        MERGED,

        /** Written several times and KEPT apart — {@code join} twice is two tables. */
        MANY
    }

    /**
     * ⚠️ Gaps of ten, so a clause can be slotted between two existing ones without renumbering either.
     */
    public static final int STEP = 10;

    public ClauseKind {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("a clause needs a keyword");
        }

        if (capability == null) {
            throw new IllegalArgumentException(
                    "clause '%s' must name the capability a backend needs in order to honour it"
                            .formatted(keyword));
        }
    }

    /** A clause that may be written once. */
    public static ClauseKind of(String keyword, Capability capability, int order) {
        return new ClauseKind(keyword, capability, order, Repetition.ONCE);
    }

    /** Whether a second one of these is legal at all. */
    public boolean repeatable() {
        return repetition != Repetition.ONCE;
    }

    /**
     * The same clause, sayable more than once.
     *
     * <p>⚠️ Repeats are an {@code and}, and a clause only declares this when its
     * {@link ClauseNode#merge(ClauseNode)} genuinely combines two of them. Declaring it without that
     * would keep the second one and drop the first, silently.</p>
     */
    public ClauseKind repeating() {
        return new ClauseKind(keyword, capability, order, Repetition.MERGED);
    }

    /**
     * The same clause, sayable more than once and kept APART.
     *
     * <p>⚠️ For a clause whose repeats are separate things rather than one combined thing — two joins are
     * two tables. Nothing merges, and a reader asking the block for "the" one of these gets the first,
     * which is why such a clause is read through a plural accessor and never a singular one.</p>
     */
    public ClauseKind many() {
        return new ClauseKind(keyword, capability, order, Repetition.MANY);
    }

    @Override
    public String toString() {
        return keyword;
    }
}
