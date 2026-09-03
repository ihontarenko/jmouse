package org.jmouse.search;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 🔤 What somebody typed, as the words a search actually asks about.
 *
 * <h3>⚠️ Why a query is not passed around as a string</h3>
 *
 * <p>Because a string is asked about as a <em>phrase</em>, and a phrase is almost never what was meant.
 * Two products in this family shipped that mistake independently: one looked for the whole query with a
 * single {@code LIKE '%…%'}, the other compared the whole query to each field. In both, <em>jmp
 * policy</em> meant those eleven characters in that order — a document carrying both words two
 * paragraphs apart did not match, and the reader concluded the wiki did not have it.</p>
 *
 * <p>Every term is its own question, and a thing answers when it answers all of them.</p>
 *
 * <h3>⚠️ Across fields, not within one</h3>
 *
 * <p>A term may be satisfied by the title while another is satisfied by the body. Requiring both in the
 * same column refuses the ordinary case of a page called <em>jMP</em> that explains policies — which is
 * why {@link Relevance} tracks which terms were answered across the whole reckoning rather than asking
 * it of one field.</p>
 *
 * <h3>⚠️ Folded once, here</h3>
 *
 * <p>Everything downstream compares against these, so the case folding happens in one place rather than
 * at each comparison — where one forgotten {@code toLowerCase} is a search that works for everybody
 * except the person who typed a capital.</p>
 *
 * @param phrase the whole query, folded and whitespace-normalised — for the one signal a bag of words
 *               cannot express, that a field reads exactly what somebody typed
 * @param terms  the distinct words, folded, in the order they were written
 */
public record SearchTerms(String phrase, List<String> terms) {

    /**
     * ⚠️ A cap rather than a validation error. Each term is another predicate in a query and another
     * pass over every candidate; a sentence pasted into the box would otherwise become thirty of them.
     * The first few words are what the person meant anyway.
     */
    public static final int MOST_TERMS_WORTH_ASKING_ABOUT = 8;

    /** Nothing was asked. */
    public static final SearchTerms NONE = new SearchTerms("", List.of());

    public SearchTerms {
        terms = List.copyOf(terms);
    }

    /** The words in a query, folded and de-duplicated. Blank in gives {@link #NONE} out. */
    public static SearchTerms of(String query) {
        if (query == null || query.isBlank()) {
            return NONE;
        }

        String folded = query.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");

        List<String> terms = Arrays.stream(folded.split(" "))
                .filter(word -> !word.isEmpty())
                .distinct()
                .limit(MOST_TERMS_WORTH_ASKING_ABOUT)
                .toList();

        return new SearchTerms(folded, terms);
    }

    /**
     * ⚠️ <strong>Empty means "answer nothing", never "answer everything".</strong> A search box that
     * lists the whole catalogue the moment it is cleared is a screen nobody asked for, and through a tool
     * it is a model reading an installation because it called an action with no argument.
     */
    public boolean empty() {
        return terms.isEmpty();
    }

    public int size() {
        return terms.size();
    }

    /**
     * One term to narrow a query with, when the query can only take one.
     *
     * <h3>⚠️ Why a single term is a CORRECT filter, not a shortcut</h3>
     *
     * <p>Some products cannot ask the loose "any term" question — their candidate query carries
     * visibility rules, joins or a hand-written {@code @Query}, and re-expressing all of that as a
     * dynamic predicate is a bigger change than the search is worth. Those callers can filter on one term
     * and lose nothing, <strong>provided the fields the query searches are among the fields the
     * reckoning weighs</strong>.
     *
     * <p>The reason: {@link Relevance#matched()} requires <em>every</em> term to appear somewhere among
     * the weighed fields. So anything that will ultimately match must contain <em>this</em> term in those
     * fields too — filtering on it can only remove rows that were going to be rejected anyway.</p>
     *
     * <p>⚠️ <strong>The proviso is load-bearing.</strong> If the reckoning weighs a field the query does
     * not search — a body the query never looks at — the filter is no longer a superset and real answers
     * disappear. Use the loose predicate there.
     *
     * <p>The longest term, because it is the most selective one available without asking the database
     * anything.
     */
    public String mostSelective() {
        return terms.stream()
                .max(java.util.Comparator.comparingInt(String::length))
                .orElse("");
    }

    /** Whether every term appears in at least one of these — the AND a multi-word query means. */
    public boolean matchedBy(String... texts) {
        return !empty() && terms.stream().allMatch(term -> containedIn(term, texts));
    }

    /** Whether any term appears in any of these — enough to be worth ranking. */
    public boolean touchedBy(String... texts) {
        return terms.stream().anyMatch(term -> containedIn(term, texts));
    }

    /** How many distinct terms this text carries. */
    public int presentIn(String text) {
        return (int) terms.stream().filter(term -> contains(text, term)).count();
    }

    /**
     * How often the terms occur in this text, counted together.
     *
     * <p>⚠️ Whoever ranks on this must cap it, or the longest document always wins — which is a ranking
     * by length wearing a ranking by relevance's clothes. {@link LadderScorer} does.
     */
    public int occurrencesIn(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        String folded = text.toLowerCase(Locale.ROOT);
        int    found  = 0;

        for (String term : terms) {
            int at = folded.indexOf(term);

            while (at >= 0) {
                found++;
                at = folded.indexOf(term, at + term.length());
            }
        }

        return found;
    }

    private static boolean containedIn(String term, String... texts) {
        return Arrays.stream(texts).anyMatch(text -> contains(text, term));
    }

    private static boolean contains(String text, String term) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(term);
    }

}
