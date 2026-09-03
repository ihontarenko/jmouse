package org.jmouse.search;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * 🧭 Every provider, asked at once and merged into one ranked answer.
 *
 * <h3>⚠️ The merge is in memory BY CONSTRUCTION, and that is said out loud</h3>
 *
 * <p>There is no way to order rows from four different tables without bringing them together somewhere,
 * and this is the somewhere. It is honest at a few hundred rows and it is not a plan for a million —
 * which is why each provider is given a {@code limit} rather than being asked for everything: the
 * narrowing happens where the rows are, and only the shortlists meet here.</p>
 *
 * <p>The failure mode this exists to prevent is the one shipped in a product already: every provider
 * returning every hit, all of it sorted, and twenty taken off the top.</p>
 *
 * <h3>⚠️ Ordering is supplied, and score-descending is only the default</h3>
 *
 * <p>Not every signal is text. A tracker wants the issues assigned to the person doing the search near
 * the top; a media library wants what somebody is halfway through. Those are orderings, not scores, and
 * a library that only sorts by a number forces them to be smuggled in as one.</p>
 *
 * @param <C> what a search is confined to — passed through to every provider unchanged
 */
public final class SearchDirectory<C> {

    /** Best first. The tie-break is deliberately absent — an unstable tie is the caller's to settle. */
    public static final Comparator<SearchHit> BEST_FIRST =
            Comparator.comparingDouble(SearchHit::score).reversed();

    private final List<SearchProvider<C>> providers;

    public SearchDirectory(Collection<? extends SearchProvider<C>> providers) {
        this.providers = List.copyOf(providers);
    }

    /** What kinds there are to filter by. */
    public List<SearchType> types() {
        return providers.stream().map(SearchProvider::describe).toList();
    }

    /** Every kind, ranked together. */
    public List<SearchHit> search(SearchTerms terms, C context, int limit) {
        return search(terms, context, limit, List.of(), BEST_FIRST);
    }

    /**
     * The same, narrowed to some kinds and ordered as the caller wants.
     *
     * @param kinds the {@code type} words to include; empty means every kind. ⚠️ A word no provider
     *              claims is silently nothing rather than an error — a filter is a request, and an
     *              interface that offered a stale one should show an empty list, not a failure.
     */
    public List<SearchHit> search(
            SearchTerms terms, C context, int limit,
            Collection<String> kinds, Comparator<SearchHit> order) {

        if (terms.empty() || limit <= 0) {
            return List.of();
        }

        return providers.stream()
                .filter(provider -> kinds.isEmpty() || kinds.contains(provider.type()))
                // ⚠️ Each provider is asked for the whole limit, not its share. Which kind holds the
                // best answers is not knowable in advance, and dividing the budget evenly is how the
                // one relevant page loses its place to four irrelevant files.
                .flatMap(provider -> provider.search(terms, context, limit).stream())
                .sorted(order)
                .limit(limit)
                .toList();
    }

}
