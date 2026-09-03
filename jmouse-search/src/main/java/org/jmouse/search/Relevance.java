package org.jmouse.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 🧮 How well one thing answers a search — field by field, and able to say so.
 *
 * <h3>⚠️ Fields are weighed and SUMMED, never maximised</h3>
 *
 * <p>The obvious implementation scores each field and takes the best one. A product in this family did
 * exactly that, and it is wrong in a way that stays invisible until somebody wonders why the right row
 * is fourth: with no weights, a whole-query match in a <em>description</em> outranks a match inside a
 * <em>name</em>. Whichever field happened to match better wins, and "better" was measured on a scale
 * that does not know a title from a footnote.</p>
 *
 * <p>So each field carries a weight, its own match is scored in [0, 1], and the contributions add up.
 * Adding rather than maximising also gets the other half right for free: a document whose title
 * <em>and</em> body carry the words is a better answer than one where only the title does, and a maximum
 * cannot say so.</p>
 *
 * <h3>⚠️ Several REPRESENTATIONS of one field compete and count once</h3>
 *
 * <p>{@link #weigh(String, double, String...)} takes more than one text, and that looks like the very
 * maximum this class exists to abolish. It is the opposite case. There, two <em>different</em> fields
 * were compared on a scale that did not know one from the other; here there is one field held in more
 * than one spelling, and both spellings are the same fact:</p>
 *
 * <ul>
 *   <li>a value shown as {@code 100 nF} and stored as {@code 100|nF} — somebody may type either</li>
 *   <li>a reference showing a person's name and storing their identifier</li>
 *   <li>a title in one language and its original in another; a film's alternative titles</li>
 * </ul>
 *
 * <p><strong>Summing them would count one field twice</strong> and lift a row with two spellings above a
 * row with one, for no reason a reader could see. So the representations compete and the field
 * contributes once — while separate fields still add, which is the entire point of the weights.</p>
 *
 * <h3>⚠️ Matching is asked across the whole reckoning, never of one field</h3>
 *
 * <p>One term may be answered by the title and another by the body, and neither field on its own can
 * report a match. That is why the answered terms are accumulated here rather than returned per field.</p>
 *
 * <h3>⚠️ The score is a PROJECTION of the rows, not the state</h3>
 *
 * <p>{@link #matches()} is the real answer and {@link #score()} sums it. Keep it that way: the moment
 * the rows are dropped and only the number is kept, a search can rank and can no longer explain, and no
 * amount of later work puts the reasons back.</p>
 */
public final class Relevance {

    private final SearchTerms      terms;
    private final Scorer           scorer;
    private final List<FieldMatch> matches = new ArrayList<>();
    private final List<Bonus>      bonuses = new ArrayList<>();

    /**
     * ⚠️ Which terms have been seen ANYWHERE, not in which field — see the note on matching above.
     */
    private final Set<String> answered = new HashSet<>();

    private Relevance(SearchTerms terms, Scorer scorer) {
        this.terms  = terms;
        this.scorer = scorer;
    }

    /** A reckoning about these terms, using the default ladder. */
    public static Relevance of(SearchTerms terms) {
        return new Relevance(terms, LadderScorer.INSTANCE);
    }

    /** The same, scored by something else — see {@link Scorer} for why this is the point. */
    public static Relevance of(SearchTerms terms, Scorer scorer) {
        return new Relevance(terms, scorer);
    }

    /**
     * Add one field to the reckoning.
     *
     * <p>A null or blank text is not an error and not a zero-scoring field — it is a field the thing does
     * not have. It is recorded as missed so the explanation can say it was asked, and it neither adds nor
     * subtracts.
     */
    public Relevance weigh(String field, double weight, String... representations) {
        Scorer.FieldScore best = Scorer.FieldScore.NONE;

        for (String text : representations) {
            if (text == null || text.isBlank()) {
                continue;
            }

            // ⚠️ The union, unlike the score: a term found in ANY spelling is a term the thing carries.
            // Somebody who typed the stored form and somebody who typed the shown form are looking at
            // the same row.
            for (String term : terms.terms()) {
                if (text.toLowerCase(Locale.ROOT).contains(term)) {
                    answered.add(term);
                }
            }

            Scorer.FieldScore scored = scorer.score(terms, text);

            if (scored.base() > best.base()) {
                best = scored;
            }
        }

        matches.add(new FieldMatch(field, weight, best.kind(), best.base(), best.occurrences()));

        return this;
    }

    /**
     * Something outside the text that makes this a better answer — recency, popularity, being yours.
     *
     * <p>⚠️ <strong>A bonus does not count towards having matched.</strong> It can only reorder things
     * that already answered the question; one that could put a non-match into the results would be a
     * search returning rows for a reason nobody typed. This is the seam for the fourth kind of signal —
     * the one that is not text at all.
     *
     * @param points already on the {@link Weights} scale
     */
    public Relevance boost(String reason, double points) {
        bonuses.add(new Bonus(reason, points));

        return this;
    }

    /**
     * Whether every term was found in at least one field — the AND a multi-word query means.
     *
     * <p>⚠️ <strong>Across fields, not within one.</strong> A page called <em>jMP</em> explaining
     * policies answers "jmp policy", and requiring both words in the same column would refuse it.
     */
    public boolean matched() {
        return !terms.empty() && answered.size() == terms.size();
    }

    /** Whether anything matched at all — a looser question, for a caller that ranks partial answers. */
    public boolean touched() {
        return !answered.isEmpty();
    }

    public double score() {
        return matches.stream().mapToDouble(FieldMatch::contribution).sum()
             + bonuses.stream().mapToDouble(Bonus::points).sum();
    }

    /** The rows, in the order they were weighed. This is the answer; the score is its sum. */
    public List<FieldMatch> matches() {
        return List.copyOf(matches);
    }

    public List<Bonus> bonuses() {
        return List.copyOf(bonuses);
    }

    /** Which of the query's words this thing actually carries. */
    public Set<String> answeredTerms() {
        return Set.copyOf(answered);
    }

    /**
     * Why this is in the answer, in one readable line — the fields that contributed something, best
     * first, and the bonuses after them.
     *
     * <p>Fields that were asked and missed are left out here: they belong in {@link #matches()}, where a
     * screen can show the whole reckoning, and they are noise in a sentence.
     */
    public String explain() {
        String contributing = matches.stream()
                .filter(match -> match.contribution() > 0)
                .sorted((first, second) ->
                        Double.compare(second.contribution(), first.contribution()))
                .map(FieldMatch::explain)
                .collect(Collectors.joining(", "));

        // ⚠️ Locale.ROOT — see FieldMatch.explain(). A diagnostic that changes its decimal separator
        // with the machine's language is a diagnostic two people cannot compare.
        String boosted = bonuses.stream()
                .map(bonus -> String.format(Locale.ROOT, "%s +%.2f", bonus.reason(), bonus.points()))
                .collect(Collectors.joining(", "));

        if (contributing.isEmpty()) {
            return boosted.isEmpty() ? "no match" : boosted;
        }

        return boosted.isEmpty() ? contributing : contributing + ", " + boosted;
    }

    /** A reason this ranks higher that is not about its text. */
    public record Bonus(String reason, double points) {
    }

}
