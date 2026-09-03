package org.jmouse.search;

/**
 * ⚖️ How much each kind of field is worth when something is scored.
 *
 * <h3>⚠️ Named constants, because the SCALE is the contract</h3>
 *
 * <p>Two things are comparably ranked only while both were weighed against the same numbers. The moment
 * one caller writes {@code 3.0} for a title and another writes {@code 10.0}, their scores stop meaning
 * the same thing — and nothing anywhere says so, because both searches go on working. So the numbers
 * live here and a call site names a <em>role</em> rather than a figure.</p>
 *
 * <h3>⚠️ Spaced apart rather than adjacent</h3>
 *
 * <p>A gap of a factor of two between rungs means a strong match one rung down cannot overtake a weak
 * match one rung up: a word buried in a document must not beat the same word in the title, however often
 * it is repeated. Rungs at 4, 3, 2, 1 would let it.</p>
 *
 * <h3>⚠️ Why these and not normalisation</h3>
 *
 * <p>The alternative was for every provider to answer in [0, 1], so that a federated search could merge
 * them. It was rejected: normalising divides by what a provider happens to weigh, so a provider matching
 * a title out of four fields scores below one matching a title out of one — the answer gets worse the
 * more the provider knows about its own rows. A shared scale is comparable <em>because the scale is
 * shared</em>. See {@code docs/adr/} for the full argument.</p>
 */
public final class Weights {

    /**
     * Something somebody quotes to name the thing: an address, a key, an identifier, a part number.
     *
     * <p>Whoever typed it is not describing what they want — they are naming it, and that is as
     * unambiguous as a search ever gets.
     */
    public static final double CRITICAL = 8.0;

    /** What the thing is called. The ordinary strongest signal. */
    public static final double PRIMARY = 4.0;

    /** A summary, a subtitle, a label — words chosen to describe it, by somebody, on purpose. */
    public static final double SECONDARY = 2.0;

    /**
     * The body. Long, unstructured and the least deliberate — a word in here may be the point of the
     * document or an aside in its fourteenth paragraph, and nothing in the text says which.
     */
    public static final double SUPPORTING = 1.0;

    /** Where it sits, who wrote it, what it is tagged with — context rather than content. */
    public static final double CONTEXTUAL = 0.5;

    private Weights() {
    }

}
