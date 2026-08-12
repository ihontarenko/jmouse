package org.jmouse.access;

/**
 * Fills in the {@code ${…}} a declaration left verbatim.
 *
 * <p>Two things reach for this and they are further apart than they look. A shipped
 * {@code bootstrap.jmp} names the founding account without a customer's identifier being compiled
 * into the artefact; a route saying {@code @AccessValue(name = "tier", is = "${innoventa.tier}")}
 * states a constant that differs per installation without the code differing per installation.
 * One seam serves both, so one bean answers both.
 *
 * <p><strong>Resolved once, at load.</strong> A policy — or a declaration — whose meaning depends on
 * runtime state is one nobody can read. What arrives here is configuration: fixed before the first
 * request and identical for every one of them.
 *
 * <p>⚠️ It lives in the engine rather than in {@code jmouse-access-policy} because
 * {@code jmouse-access-enforcement} needs it too and depends on this module and slf4j and nothing
 * else. The implementation is somebody else's: {@code EnvironmentPlaceholders} in the Spring starter
 * reads Spring's {@code Environment}, a game reads whatever it keeps its settings in.
 *
 * <h2>⚠️ Why this is not {@code org.jmouse.core.PlaceholderResolver}</h2>
 *
 * <p>It is the same idea and deliberately the same shape, and it is <strong>not</strong> that
 * interface because this module's dependency list is {@code slf4j} and nothing else. Reaching for
 * core would bring byte-buddy and objenesis behind it, so every product adopting the engine would
 * inherit a proxy engine to read one property — which is the same argument that keeps
 * {@code QuantityScale} a seam rather than a unit parser.
 *
 * <p>An installation that <em>does</em> have core on its classpath spends one lambda to bridge them:
 * {@code key -> core.resolvePlaceholder(key, null)}. That is the whole cost of the duplication, and
 * it is paid by the adapter rather than by every consumer of the engine.
 */
@FunctionalInterface
public interface PlaceholderResolver {

    /**
     * A resolver that refuses every placeholder.
     *
     * <p>The default, and fail-closed on purpose: leaving {@code ${owner}} in place as a literal
     * would create a grant belonging to an account that can never exist, which is harmless right up
     * until somebody registers with that name.
     */
    static PlaceholderResolver none() {
        return key -> {
            throw new IllegalStateException(
                    "This declaration uses the placeholder ${" + key + "}, and no resolver was "
                    + "supplied. Placeholders are filled from application configuration at load; "
                    + "without one the value would be read literally and mean nothing.");
        };
    }

    /**
     * Fills every {@code ${…}} in one piece of text, or answers it unchanged where it holds none.
     *
     * <p>Here rather than at each call site because every caller wants the same thing and the loop
     * is exactly the sort of four lines that comes to differ between two copies of it.
     *
     * @param text     what was written, possibly with placeholders in it
     * @param resolver what to fill them from
     * @return the text with every placeholder replaced
     */
    static String fill(String text, PlaceholderResolver resolver) {
        if (text == null || !text.contains("${")) {
            return text;
        }

        StringBuilder filled = new StringBuilder(text.length());
        int           cursor = 0;

        while (true) {
            int opening = text.indexOf("${", cursor);

            if (opening < 0) {
                break;
            }

            int closing = text.indexOf('}', opening + 2);

            if (closing < 0) {
                throw new IllegalStateException(
                        "'" + text + "' opens a placeholder that never closes. Write ${key}, or drop "
                        + "the '${' — a half-written placeholder is read literally, which is how a "
                        + "value comes to be compared against the word '${key' forever.");
            }

            filled.append(text, cursor, opening)
                  .append(resolver.resolve(text.substring(opening + 2, closing)));

            cursor = closing + 1;
        }

        return filled.append(text, cursor, text.length()).toString();
    }

    /** @throws RuntimeException where the property is not set — an unresolved placeholder is a fault */
    String resolve(String key);
}
