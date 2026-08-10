package org.jmouse.access.policy;

/**
 * Fills in the {@code ${…}} a file left verbatim.
 *
 * <p>Exists so that a shipped {@code bootstrap.jmp} can name the founding account without a customer's
 * identifier being compiled into the artefact. Resolved once, at load — a policy whose meaning depends
 * on runtime state is a policy nobody can read.
 */
@FunctionalInterface
public interface PlaceholderResolver {

    /**
     * A resolver that refuses every placeholder.
     *
     * <p>The default, and fail-closed on purpose: leaving {@code ${owner}} in place as a literal
     * subject identifier would create a grant belonging to an account that can never exist, which is
     * harmless right up until somebody registers with that name.
     */
    static PlaceholderResolver none() {
        return key -> {
            throw new PolicyException(
                    "This policy uses the placeholder ${" + key + "}, and no resolver was supplied. "
                    + "Placeholders are filled from application configuration at load; without one "
                    + "the value would be stored literally and grant to nobody.");
        };
    }

    /** @throws PolicyException where the property is not set — an unresolved placeholder is a fault */
    String resolve(String key);
}
