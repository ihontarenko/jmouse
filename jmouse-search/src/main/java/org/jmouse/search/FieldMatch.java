package org.jmouse.search;

/**
 * 📐 One field's answer to the query — named, weighed, and able to say how it matched.
 *
 * <h3>⚠️ A row rather than a number, and this is the whole design</h3>
 *
 * <p>The obvious shape is for a scorer to return a {@code double} and for the reckoning to add it up.
 * That shape cannot answer <em>why is this in front of me</em>, which is a thing at least one product's
 * search screen is specified to do — and it cannot be retro-fitted, because by the time the sum exists
 * the reasons are gone.</p>
 *
 * <p>So a field contributes a row. {@link Relevance} keeps the rows and the score is their sum; an
 * interface that wants the explanation reads the rows, and one that wants a number reads the sum. The
 * expensive half was never the arithmetic.</p>
 *
 * @param field       what this field is called, for the explanation — {@code "title"}, {@code "body"}
 * @param weight      its worth on the {@link Weights} scale
 * @param kind        how it matched
 * @param base        how well, in [0, 1], before the weight is applied
 * @param occurrences how many times a term appears in it — a tie-break signal, not a score
 */
public record FieldMatch(String field, double weight, MatchKind kind, double base, int occurrences) {

    /** A field that was weighed and did not answer. Kept, so the explanation can say it was asked. */
    public static FieldMatch missed(String field, double weight) {
        return new FieldMatch(field, weight, MatchKind.NONE, 0.0, 0);
    }

    /** What this field adds to the score. */
    public double contribution() {
        return weight * base;
    }

    public boolean matched() {
        return kind.matched();
    }

    /**
     * One line a person can read — {@code "title EXACT ×4.0 = 4.00"}.
     *
     * <p>⚠️ <strong>{@link java.util.Locale#ROOT}, and it is not a detail.</strong> Without it the
     * default locale decides the decimal separator, so the same explanation reads {@code ×4,0} on one
     * machine and {@code ×4.0} on another — for a string that is a diagnostic, not prose. It is not
     * translated and must not look as though it were.
     */
    public String explain() {
        return String.format(
                java.util.Locale.ROOT, "%s %s ×%.1f = %.2f", field, kind, weight, contribution());
    }

}
