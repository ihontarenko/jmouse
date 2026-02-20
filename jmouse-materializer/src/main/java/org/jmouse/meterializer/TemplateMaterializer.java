package org.jmouse.meterializer;

/**
 * Strategy responsible for converting a {@link NodeTemplate}
 * into a concrete rendered representation. ⚙️
 *
 * <p>
 * A {@code TemplateMaterializer} performs the final transformation step
 * in the rendering pipeline. It receives:
 * </p>
 * <ul>
 *     <li>a structural blueprint ({@link NodeTemplate})</li>
 *     <li>an active {@link RenderingExecution} context</li>
 * </ul>
 *
 * <p>
 * The implementation resolves expressions, evaluates predicates,
 * applies attributes, processes children, and produces a result of type {@code T}.
 * </p>
 *
 * <h3>Typical result types</h3>
 * <ul>
 *     <li>{@code String} (HTML output)</li>
 *     <li>DOM node representation</li>
 *     <li>custom UI component model</li>
 * </ul>
 *
 * <h3>Lifecycle (conceptual)</h3>
 *
 * <pre>{@code
 * resolve values → evaluate predicates → materialize children
 * → build final representation → return result
 * }</pre>
 *
 * <p>
 * Implementations should be stateless and thread-safe unless explicitly documented otherwise.
 * The {@link RenderingExecution} instance is per-invocation.
 * </p>
 *
 * @param <T> materialized result type
 */
public interface TemplateMaterializer<T> {

    /**
     * Materializes a template node into its final representation.
     *
     * @param template   node blueprint definition
     * @param execution  active rendering execution context
     * @return materialized result
     */
    T materialize(NodeTemplate template, RenderingExecution execution);

}