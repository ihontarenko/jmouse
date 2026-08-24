package org.jmouse.query.spring.builder;

/**
 * Where the shared builder answers.
 *
 * <p>⚠️ <strong>One constant, and nothing inlines it.</strong> The same address has to be written in a
 * controller, in whatever a product uses to gate it, and in the interface that calls it. Spread across
 * those, a change to one of them renders as an empty vocabulary rather than as an error — a builder with
 * no fields looks exactly like a form with no fields.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class QueryRoutes {

    /**
     * Resolved by Spring, so a product only sets it in configuration.
     *
     * <p>The default sits under the product's own API root rather than in a library corner, because this
     * is a <em>feature</em> surface a person's browser calls constantly — not an administration screen
     * somebody opens twice a year.</p>
     */
    public static final String PREFIX = "${jmouse.query.builder.prefix:/api/query}";

    private QueryRoutes() {
    }
}
