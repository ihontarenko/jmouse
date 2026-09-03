package org.jmouse.search;

/**
 * 🎯 <em>How</em> a field answered the query — the half of a match a number cannot carry.
 *
 * <h3>⚠️ This exists so a search can say why</h3>
 *
 * <p>A score of {@code 5.4} is not an explanation, and at least one product in this family has a search
 * screen specified to explain itself. Every rung here is a different question a person actually asks,
 * and naming the rung is what lets an interface write <em>"the title is exactly what you typed"</em>
 * instead of printing a decimal nobody can check.</p>
 *
 * <p>⚠️ It is also what makes a <strong>foreign</strong> ranking expressible. A provider whose score came
 * from somewhere else — a search engine, a database's own full-text ranking, a remote API — reports
 * {@link #EXTERNAL} and hands the number through. Without a rung for that, such a provider would have to
 * lie about which rung it hit.</p>
 */
public enum MatchKind {

    /** The field reads exactly what was typed. This is the thing, not a thing mentioning it. */
    EXACT,

    /** The field begins with what was typed — somebody typed the start of a name. */
    PREFIX,

    /** The words appear together, in the order they were typed. */
    PHRASE,

    /** Every word is in there, scattered. What a multi-word query usually means. */
    ALL_TERMS,

    /** Some of the words. Worth ranking, in proportion — and never worth calling a match. */
    SOME_TERMS,

    /**
     * Scored elsewhere and carried through — a database's own ranking, a remote index, a service that
     * returns relevance of its own.
     *
     * <p>⚠️ The contribution is taken at face value. Whoever reports this is asserting the number is
     * already on {@link Weights}' scale; nothing here can check that, which is exactly why it is a
     * distinct rung rather than a silent default.
     */
    EXTERNAL,

    /** Nothing. Kept as a value so a field that was weighed and missed is still a row in the answer. */
    NONE;

    public boolean matched() {
        return this != NONE && this != SOME_TERMS;
    }

}
