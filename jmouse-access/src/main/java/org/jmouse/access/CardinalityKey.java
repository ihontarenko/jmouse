package org.jmouse.access;

/**
 * What is being counted, and where — the key a {@link org.jmouse.access.spi.CardinalityCounters}
 * implementation answers about.
 *
 * <h2>⚠️ Two axes, and neither is the subject</h2>
 *
 * <p>A {@link ConsumptionKey} carries a subject, because <em>how much has this account spent</em> is a
 * question about a person. This one does not: <em>how many projects exist in this space</em> is a
 * question about a <strong>place</strong>, and the answer is the same whoever asks it.
 *
 * <p>That is also why a seat limit works: the count is of the place's rows, so it does not matter which
 * member is the one trying to add the twenty-first.
 *
 * @param kind  what is being counted — {@code project}, {@code member}, {@code form}. A name the product
 *              declares; a name it does not is refused at load
 * @param place where to count it
 */
public record CardinalityKey(String kind, ScopeReference place) {

    public CardinalityKey {
        if (kind == null || kind.isBlank()) {
            throw new IllegalArgumentException("a count has to say what it is counting");
        }

        if (place == null) {
            throw new IllegalArgumentException(
                    "a count of '" + kind + "' has to say where — a count with no place is a count of "
                    + "everything, which is never what a limit means");
        }
    }

    /** How this reads in a refusal. */
    public String describe() {
        return kind + " in " + place.describe();
    }
}
