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
 * @param repeatable whether saying it twice is legal, and therefore an {@code and} rather than a mistake
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record ClauseKind(String keyword, Capability capability, int order, boolean repeatable) {

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
        return new ClauseKind(keyword, capability, order, false);
    }

    /**
     * The same clause, sayable more than once.
     *
     * <p>⚠️ Repeats are an {@code and}, and a clause only declares this when its
     * {@link ClauseNode#merge(ClauseNode)} genuinely combines two of them. Declaring it without that
     * would keep the second one and drop the first, silently.</p>
     */
    public ClauseKind repeating() {
        return new ClauseKind(keyword, capability, order, true);
    }

    @Override
    public String toString() {
        return keyword;
    }
}
