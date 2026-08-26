package org.jmouse.query.spring.builder;

import org.jmouse.core.management.ManagementEndpoints;

/**
 * Where the shared builder answers.
 *
 * <p>⚠️ <strong>One constant, and nothing inlines it.</strong> The same address has to be written in a
 * controller, in whatever a product uses to gate it, and in the interface that calls it. Spread across
 * those, a change to one of them renders as an empty vocabulary rather than as an error — a builder with
 * no fields looks exactly like a form with no fields.</p>
 *
 * <h2>⚠️ It moved under {@code /jmouse}, and the earlier reasoning is worth reading before moving it back</h2>
 *
 * <p>The default was {@code /api/query} — deliberately inside the product's own API root, on the grounds
 * that this is a <em>feature</em> surface a person's browser calls constantly rather than an
 * administration screen somebody opens twice a year. That argument is about how the address <em>reads</em>,
 * and it lost to a plainer one: a library publishing into the product's URL space is exactly what
 * {@link ManagementEndpoints} exists to prevent, and having one library answer by a different convention
 * to the rest means every product's proxy, security matcher and interface carries a second rule. Ivan,
 * 2026-08-25: <em>«нехай всі бібліотечні будуть уніфіковані типу /jmouse/…»</em>.</p>
 *
 * <p>So it now composes like every other module: {@code /jmouse/query/api} by default,
 * {@code jmouse.query.management.prefix} to move this one, {@code jmouse.management.prefix} to move them
 * all. ⚠️ The property was {@code jmouse.query.builder.prefix} — a product that had set that one is
 * silently back on the default, which is why nothing in this workspace sets it and why the rename is
 * safe here and worth announcing anywhere else.</p>
 */
public final class QueryRoutes {

    /**
     * Resolved by Spring, so a product only sets it in configuration.
     *
     * <p>Composed the way {@link ManagementEndpoints} documents: the module's own property wins, the
     * shared root answers next, and the compiled-in default answers otherwise.
     */
    public static final String PREFIX =
            "${jmouse.query.management.prefix:" + ManagementEndpoints.ROOT + "/query/api}";

    private QueryRoutes() {
    }
}
