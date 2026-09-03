package org.jmouse.search;

/**
 * 🎚️ How well one text answers the query — <strong>the seam this whole library exists to provide</strong>.
 *
 * <h3>⚠️ The formula is an implementation, and it is deliberately not here</h3>
 *
 * <p>A library that hard-codes <em>0.8 for a prefix</em> has taken one team's taste and made every
 * product live with it — and moved the magic numbers somewhere harder to change than where they started.
 * What is shared between products is that a query becomes words, that fields are weighed, and that a
 * match can explain itself. What is <em>not</em> shared is how good a prefix is.</p>
 *
 * <p>So: an interface, with {@link LadderScorer} as a default that is sane rather than authoritative.
 * A product that wants BM25, a language-aware stemmer, transliteration between an alphabet and its
 * romanisation, or a flat first-match-wins implements this and changes nothing else.</p>
 *
 * <h3>⚠️ It answers a KIND, not only a number</h3>
 *
 * <p>Returning a bare {@code double} would put the seam back where it started: the caller could rank but
 * not explain, and every scorer would have to agree on what {@code 0.65} means before an interface could
 * say anything about it. A {@link MatchKind} is a word both sides already understand.</p>
 */
@FunctionalInterface
public interface Scorer {

    /**
     * How this text answers these terms.
     *
     * @return the rung it reached and how well, in [0, 1] — never null; use
     *         {@link FieldScore#NONE} for no answer at all
     */
    FieldScore score(SearchTerms terms, String text);

    /**
     * A scorer's answer about one text, before any weight is applied.
     *
     * @param kind        which rung
     * @param base        how well, in [0, 1]
     * @param occurrences how often the terms occur — carried so a caller can tie-break without
     *                    re-scanning the text
     */
    record FieldScore(MatchKind kind, double base, int occurrences) {

        public static final FieldScore NONE = new FieldScore(MatchKind.NONE, 0.0, 0);

        public FieldScore {
            base = Math.max(0.0, Math.min(1.0, base));
        }

        /**
         * A score computed somewhere else — a database's ranking, a remote index.
         *
         * <p>⚠️ The caller is asserting this is already on the same scale as everything else it will be
         * merged with. Nothing here can check that, which is why it is a named factory rather than a
         * number quietly passed in.
         */
        public static FieldScore external(double base) {
            return new FieldScore(MatchKind.EXTERNAL, base, 0);
        }
    }

}
