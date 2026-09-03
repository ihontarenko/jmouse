package org.jmouse.search.text;

import org.jmouse.search.Relevance;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * 📊 Candidates in, ranked answers out — the six lines every document search writes identically.
 *
 * <h3>⚠️ Deliberately small, and deliberately not a framework</h3>
 *
 * <p>What is shared between a wiki searching pages and a tracker searching issues is not much: weigh
 * each candidate, drop the ones that did not match, order by score with a tie-break, cut to the limit.
 * That is worth owning once because everybody writes it slightly differently and one of the differences
 * is always a bug — but it is not worth a pipeline of callbacks. Everything above it (what the
 * candidates are, how they were queried) and below it (what a hit looks like) stays the product's.</p>
 *
 * <h3>⚠️ The rule this encodes: NARROW in the query, DECIDE here</h3>
 *
 * <p>The database should be asked a loose question — rows carrying <strong>any</strong> term — and the
 * real rule, <em>every term, in any field</em>, applied once in Java by {@link Relevance#matched()}.
 * It is tempting to write the strict AND into the query and skip this pass. It is also two
 * implementations of one rule that must agree forever, and the way they stop agreeing is a row one
 * surface finds and another does not.</p>
 *
 * <h3>⚠️ Recency is a TIE-BREAK, never the sort</h3>
 *
 * <p>Ordering by "most recently changed" is what both products in this family shipped, and it is why a
 * passing mention on something edited yesterday outranked a title. The tie-break is where that signal
 * belongs — and a stronger non-text signal belongs in {@link Relevance#boost}, where it is visible in
 * the explanation rather than hidden in a comparator.</p>
 */
public final class DocumentRanking {

    private DocumentRanking() {
    }

    /**
     * The candidates that answered, best first.
     *
     * @param weigh    builds the reckoning for one candidate — where the product's own weights live
     * @param tieBreak how to order two candidates that scored the same; recency is the usual answer
     */
    public static <T> List<Ranked<T>> best(
            Collection<T> candidates,
            Function<T, Relevance> weigh,
            Comparator<T> tieBreak,
            int limit) {

        if (candidates.isEmpty() || limit <= 0) {
            return List.of();
        }

        return candidates.stream()
                .map(candidate -> new Ranked<>(candidate, weigh.apply(candidate)))
                .filter(ranked -> ranked.relevance().matched())
                .sorted(Comparator
                        .comparingDouble((Ranked<T> ranked) -> ranked.relevance().score())
                        .reversed()
                        .thenComparing(Ranked::subject, tieBreak))
                .limit(limit)
                .toList();
    }

    /**
     * One candidate and what was worked out about it.
     *
     * <p>⚠️ The reckoning is kept rather than reduced to its score, so the caller can put the
     * explanation on the hit without weighing anything twice.
     */
    public record Ranked<T>(T subject, Relevance relevance) {

        public double score() {
            return relevance.score();
        }
    }

}
