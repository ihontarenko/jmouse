package org.jmouse.access;

import java.util.Optional;

/**
 * One kind of scope a grant can be made at — the model's side of what {@code AccessScope} spells out
 * for this product.
 *
 * <p>The engine used to read a closed enum, which meant that "how many floors are there, and what are
 * they called" was a fact compiled into the authorization core. A product with departments, or
 * tenants, or nothing at all between an account and a row could not use that core without editing it.
 * This interface is the same information asked as a question instead: a name, a width, whether the
 * scope names an instance, and {@linkplain ScopeNature what it is to the model}.
 *
 * <p><strong>Implement it with an enum.</strong> Nothing here needs one, but a product wants one:
 * enum constants are usable in annotations ({@code @RequiresAccess(scope = AccessScope.SPACE)}),
 * mappable by JPA as {@code @Enumerated(STRING)}, and usable in Spring Data derived queries — none of
 * which an open value type can do. So the product keeps its enum and its ergonomics, and the core
 * gets its ignorance; {@code AccessScope} is exactly that arrangement, and its {@link #rank()} is
 * {@code ordinal()}.
 *
 * <p>The set of kinds that exist is {@link ScopeCatalog}, which is where the ordering is checked and
 * where the two universal scopes are found. Nothing derives the vocabulary by scanning an enum's
 * constants, because the point is that the core does not know which enum to scan.
 */
public interface ScopeKind {

    /** The value stored where a scope kind has no instance to name — {@code EVERYTHING}, {@code OWN_ROWS}. */
    String NO_INSTANCE = "*";

    /**
     * What this kind is called, in grants, in the database and in a refusal.
     *
     * <p>An enum gets this for free. It is the stored form, so renaming a constant is a migration.
     */
    String name();

    /**
     * How wide this kind is: {@code 0} is the widest, and larger is narrower.
     *
     * <p>Ranks have to be unique across a vocabulary and are otherwise arbitrary — they are read only
     * against each other, never as a count. An enum returns {@code ordinal()} and gets a correct,
     * gap-free ordering from its declaration order, which is why the declaration order of
     * {@code AccessScope} is documented as being read.
     */
    int rank();

    /** What this kind is to the model — the question a product cannot leave unanswered. */
    ScopeNature nature();

    /**
     * The path variable or query parameter a route names an instance of this kind with.
     *
     * <p>Declared beside the scope rather than as a constant in the enforcement package, so that
     * {@code AccessTargetBinder} holds no product vocabulary at all: it asks each floor what to look
     * for. Empty for the two universal kinds, which name nothing.
     */
    Optional<String> requestParameter();

    /**
     * Whether naming this kind requires an identifier — a place does, the two universal scopes do not.
     *
     * <p>Derived from {@link #nature()} rather than from whether a request parameter happens to be
     * set, so a floor that is never reachable from a URL is still a floor.
     */
    default boolean namesAnInstance() {
        return nature() == ScopeNature.PLACE;
    }

    /** Whether this kind is at least as wide as another. The widest covers everything. */
    default boolean isAtLeastAsWideAs(ScopeKind other) {
        return rank() <= other.rank();
    }
}
