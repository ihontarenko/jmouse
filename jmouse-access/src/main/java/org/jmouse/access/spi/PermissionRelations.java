package org.jmouse.access.spi;

import java.util.Optional;

/**
 * Which permissions aim somewhere other than the row they are asked about — the {@code through} clause,
 * read back.
 *
 * <p>A policy says it once, in the vocabulary rather than on a grant, because it is a property of the
 * <em>permission</em> and not of who holds it:
 *
 * <pre>
 * declare permissions {
 *     field:write "Create and edit field definitions" through each form
 * }
 * </pre>
 *
 * <p>The loader turns that into this port; {@code AccessEngine} asks it before resolving a target, and
 * every grant line in the installation stays exactly as it was.
 *
 * <p>⚠️ <strong>An installation that declares none must cost nothing</strong> — {@link #none()} is the
 * floor case, and with it the engine behaves exactly as it did before this existed.
 */
public interface PermissionRelations {

    /** What a permission's {@code through} clause said, or empty where it has none. */
    Optional<Redirect> redirectFor(String permission);

    /**
     * How many of the related rows have to allow the permission.
     *
     * <h2>⚠️ Written in the policy, never defaulted</h2>
     *
     * <p>{@code through form} with no quantifier does not parse. It briefly meant {@code each}, and that
     * was a default nobody could see — the difference it hid is not small: a field standing on forty-five
     * forms is renamed by anybody who owns <em>one</em> of them under {@link #ANY}, and by nobody short
     * of an administrator under {@link #EACH}.
     *
     * <p>⚠️ <strong>An enum rather than a boolean</strong>, because a {@code true} at a call site says
     * nothing about which way round it is — and getting that backwards is not a refusal somebody
     * notices, it is a permission handed to more people than intended.
     *
     * <p>The word is {@code each} rather than {@code all} or {@code every} because the resource is named
     * in the singular, so {@code through each form} is the only one of the three that is also English.
     * It happens to describe the mechanism exactly.
     */
    enum Quantifier {

        /** One related row allowing it is enough. */
        ANY("any"),

        /** Every one of them must allow it, and the first refusal is the answer. */
        EACH("each");

        private final String word;

        Quantifier(String word) {
            this.word = word;
        }

        /** The word a policy writes, and the one a round trip has to reproduce. */
        public String word() {
            return word;
        }

        /** The quantifier a policy meant by this word, or empty — which is a parse error, not a default. */
        public static Optional<Quantifier> byWord(String word) {
            for (Quantifier quantifier : values()) {
                if (quantifier.word.equalsIgnoreCase(word)) {
                    return Optional.of(quantifier);
                }
            }

            return Optional.empty();
        }
    }

    /**
     * One {@code through} clause, with the resource already resolved to a type.
     *
     * @param destination the resource type the check is aimed at instead
     * @param quantifier  how many of the related rows have to allow it
     */
    record Redirect(Class<?> destination, Quantifier quantifier) {

        /** Whether every related row has to allow it. */
        public boolean requiresEach() {
            return quantifier == Quantifier.EACH;
        }
    }

    /** A build whose policy declares no redirect at all. */
    static PermissionRelations none() {
        return permission -> Optional.empty();
    }
}
