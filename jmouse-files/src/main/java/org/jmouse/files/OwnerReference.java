package org.jmouse.files;

import org.jmouse.files.exception.FileBindingException;

import java.util.Locale;

/**
 * 🔗 What a file is filed against — a kind and an identifier, and nothing else.
 *
 * <h3>Why the kind is a string</h3>
 *
 * <p>Because the values belong to the product, not to this library. Tessera files against
 * {@code ISSUE} and {@code COMMENT}, Kiwi against {@code DIRECTORY}, Innoventa against
 * {@code ENTRY_FIELD}. An enum here would mean a release of this library every time a product found
 * a new thing to attach a file to, which is the opposite of what extracting it was for.</p>
 *
 * <p>⚠️ A product should still declare its own constants rather than writing the strings at call
 * sites — this type deliberately cannot check them, so a typo binds a file to a kind nothing will
 * ever query and the file simply disappears from every listing.</p>
 *
 * <h3>The shape this replaces</h3>
 *
 * <p>Three products built this independently before it was extracted — {@code EntityCategory} in two
 * of them, {@code Attachment(ownerType, ownerId)} with six owner kinds in a third. It is not a new
 * idea being introduced; it is the one they all reached for, written once.</p>
 *
 * @param ownerType what kind of thing holds the file, upper-cased
 * @param ownerId   which one, as the product's own identifier
 */
public record OwnerReference(String ownerType, String ownerId) {

    /** What separates the kind from the identifier when a reference is written as one value. */
    public static final char SEPARATOR = ':';

    /**
     * The owner kind of a file filed into a library directory — and the name of the scope that
     * authorizes it.
     *
     * <p>⚠️ One constant because it is one word written in four places otherwise: the binding, the scope
     * enum, the policy file and the access rules. Three products writing it by hand is three chances for
     * a typo that binds files somewhere nothing queries.</p>
     */
    public static final String DIRECTORY = "DIRECTORY";

    /** Longest an owner kind may be, matching the column that stores it. */
    public static final int MAXIMUM_TYPE_LENGTH = 64;

    /** Longest an owner identifier may be, matching the column that stores it. */
    public static final int MAXIMUM_ID_LENGTH = 64;

    /**
     * 🏗️ Refuse a reference that could not identify anything.
     *
     * <p>Normalised to upper case, so {@code issue} and {@code ISSUE} are one kind rather than two
     * halves of a listing.</p>
     */
    public OwnerReference {
        ownerType = required(ownerType, "owner type", MAXIMUM_TYPE_LENGTH).toUpperCase(Locale.ROOT);
        ownerId   = required(ownerId, "owner id", MAXIMUM_ID_LENGTH);
    }

    /**
     * 🏗️ A reference to one thing.
     *
     * @param ownerType what kind of thing holds the file
     * @param ownerId   which one
     * @return the reference
     */
    public static OwnerReference of(String ownerType, String ownerId) {
        return new OwnerReference(ownerType, ownerId);
    }

    /**
     * 🧭 Read a reference written as one value: {@code ISSUE:TES-42}.
     *
     * <h3>⚠️ Why one value and not two</h3>
     *
     * <p>Because an authorization rule can name exactly one parameter as the thing a route acts on. A
     * route taking {@code ownerType} and {@code ownerId} separately cannot be gated on its owner at
     * all — the rule would have to name one of them, and neither identifies anything by itself. Two
     * parameters read better and are unguardable; one reads slightly worse and can be authorized.</p>
     *
     * <p>⚠️ The identifier may itself contain a colon — {@code SPACE:workspace:7} is
     * {@code SPACE} and {@code workspace:7} — so the split is at the <em>first</em> one only.</p>
     *
     * @param reference the reference as written
     * @return the parsed reference
     */
    public static OwnerReference parse(String reference) {
        String candidate = reference == null ? "" : reference.trim();
        int    separator = candidate.indexOf(SEPARATOR);

        if (separator < 0) {
            throw new FileBindingException(
                "An owner is written <kind>%s<id>, for example ISSUE%sTES-42 — '%s' is not."
                    .formatted(SEPARATOR, SEPARATOR, candidate));
        }

        return of(candidate.substring(0, separator), candidate.substring(separator + 1));
    }

    private static String required(String value, String what, int maximumLength) {
        String candidate = value == null ? "" : value.trim();

        if (candidate.isEmpty()) {
            throw new FileBindingException("A file binding needs an %s.".formatted(what));
        }

        if (candidate.length() > maximumLength) {
            throw new FileBindingException(
                "An %s may be up to %d characters — '%s' is %d."
                    .formatted(what, maximumLength, candidate, candidate.length()));
        }

        return candidate;
    }

    @Override
    public String toString() {
        return "%s%s%s".formatted(ownerType, SEPARATOR, ownerId);
    }
}
