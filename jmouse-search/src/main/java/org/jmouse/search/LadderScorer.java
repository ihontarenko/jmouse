package org.jmouse.search;

import java.util.Locale;

/**
 * 🪜 The default {@link Scorer}: a ladder of rungs, each answering a different question a person asks.
 *
 * <h3>⚠️ Sane, not authoritative</h3>
 *
 * <p>These numbers are a starting point that behaves well, and they are <strong>not</strong> a claim
 * about how relevance works. They live behind {@link Scorer} precisely so that disagreeing with them is
 * a class rather than a fork — see that interface for why the formula is not the library's to fix.</p>
 *
 * <p>The rungs, in order:</p>
 *
 * <ul>
 *   <li><strong>equals</strong> — this is the thing, not a thing mentioning it</li>
 *   <li><strong>starts with</strong> — somebody typed the beginning of a name</li>
 *   <li><strong>contains the phrase</strong> — the words in the order they were typed</li>
 *   <li><strong>every term, scattered</strong> — what a multi-word query usually means</li>
 *   <li><strong>some terms</strong> — worth ranking, in proportion, and never worth calling a match</li>
 * </ul>
 *
 * <h3>⚠️ Repetition counts a little, and is capped</h3>
 *
 * <p>A document that says the word nine times is more about it than one that says it once. Uncapped,
 * though, the longest document always wins — which is a ranking by length wearing a ranking by
 * relevance's clothes.</p>
 */
public final class LadderScorer implements Scorer {

    /** The ordinary instance. It holds no state, so there is no reason for a second one. */
    public static final LadderScorer INSTANCE = new LadderScorer();

    private static final double EXACT_BASE      = 1.00;
    private static final double PREFIX_BASE     = 0.80;
    private static final double PHRASE_BASE     = 0.65;
    private static final double ALL_TERMS_BASE  = 0.50;
    private static final double SOME_TERMS_BASE = 0.30;

    private static final double PER_EXTRA_OCCURRENCE = 0.02;
    private static final int    OCCURRENCES_COUNTED  = 6;

    @Override
    public FieldScore score(SearchTerms terms, String text) {
        if (text == null || text.isBlank() || terms.empty()) {
            return FieldScore.NONE;
        }

        String folded      = text.toLowerCase(Locale.ROOT);
        int    present     = terms.presentIn(text);
        int    occurrences = terms.occurrencesIn(text);

        MatchKind kind;
        double    base;

        if (folded.equals(terms.phrase())) {
            kind = MatchKind.EXACT;
            base = EXACT_BASE;
        } else if (folded.startsWith(terms.phrase())) {
            kind = MatchKind.PREFIX;
            base = PREFIX_BASE;
        } else if (folded.contains(terms.phrase())) {
            kind = MatchKind.PHRASE;
            base = PHRASE_BASE;
        } else if (present == terms.size()) {
            kind = MatchKind.ALL_TERMS;
            base = ALL_TERMS_BASE;
        } else if (present > 0) {
            kind = MatchKind.SOME_TERMS;
            base = SOME_TERMS_BASE * present / terms.size();
        } else {
            return FieldScore.NONE;
        }

        int repeats = Math.max(0, Math.min(occurrences, OCCURRENCES_COUNTED) - 1);

        return new FieldScore(kind, base + repeats * PER_EXTRA_OCCURRENCE, occurrences);
    }

}
