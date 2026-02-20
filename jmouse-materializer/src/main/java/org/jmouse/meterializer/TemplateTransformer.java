package org.jmouse.meterializer;

/**
 * Pre-materialization transformation step for {@link NodeTemplate}. 🔄
 *
 * <p>
 * A {@code TemplateTransformer} allows structural or semantic modification
 * of a blueprint before it is materialized. It operates within the active
 * {@link RenderingExecution} context.
 * </p>
 *
 * <p>
 * Typical use cases:
 * </p>
 * <ul>
 *     <li>conditional node removal / replacement</li>
 *     <li>template expansion or macro processing</li>
 *     <li>dynamic attribute enrichment</li>
 *     <li>structural normalization</li>
 * </ul>
 *
 * <h3>Pipeline position</h3>
 *
 * <pre>{@code
 * blueprint → transform(...) → materialize(...)
 * }</pre>
 *
 * <p>
 * Implementations should ideally be stateless and deterministic.
 * The returned {@link NodeTemplate} may be the same instance or a modified copy.
 * </p>
 */
public interface TemplateTransformer {

    /**
     * Transforms a blueprint prior to materialization.
     *
     * @param blueprint input blueprint
     * @param execution active rendering execution context
     * @return transformed blueprint (never {@code null})
     */
    NodeTemplate transform(NodeTemplate blueprint, RenderingExecution execution);
}
