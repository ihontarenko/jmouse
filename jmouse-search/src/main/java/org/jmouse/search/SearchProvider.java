package org.jmouse.search;

import java.util.List;

/**
 * 🗂️ One kind of thing that can be found — the federation half of the library.
 *
 * <h3>⚠️ Optional, and a single-type product should not implement it</h3>
 *
 * <p>A wiki has pages and nothing else; a tracker has issues. Those products want {@link Relevance} and
 * nothing on this page. A workspace that searches forms, fields, entries and files at once wants this,
 * because merging four rankings into one list is the part everybody writes badly.</p>
 *
 * <p>So the federation is here rather than in the core of the core: implementing it is a decision, and a
 * product that has one searchable type should not be paying for a registry.</p>
 *
 * <h3>⚠️ The provider does its own query, its own visibility and its own scoring</h3>
 *
 * <p>None of those three is the library's business and each is different per product — what rows exist,
 * who may see them, and what a good score is there. What the library supplies is that they all come back
 * comparable, because they all weighed against {@link Weights}.</p>
 *
 * <h3>⚠️ The limit is passed IN, not applied afterwards</h3>
 *
 * <p>A directory that asked every provider for everything and then took twenty has loaded the whole
 * catalogue to draw one screen. Providers are told how many are wanted so the narrowing can happen where
 * the rows are — in a query, with an index, rather than in a list.
 *
 * @param <C> what a search is confined to here — a workspace, a project, a member. ⚠️ It is a type
 *            parameter rather than a map of attributes because the products differ, and a bag of
 *            strings would push the difference into every provider's first three lines.
 */
public interface SearchProvider<C> {

    /** The word this kind of thing is called — matches {@link SearchHit#type()}. */
    String type();

    /** What a person calls it, for a type filter. */
    String label();

    /** How it is drawn beside its label. */
    String icon();

    /** Everything of this kind answering these terms, within this context, at most {@code limit}. */
    List<SearchHit> search(SearchTerms terms, C context, int limit);

    /** This provider as an interface's type filter describes it. */
    default SearchType describe() {
        return new SearchType(type(), label(), icon());
    }

}
