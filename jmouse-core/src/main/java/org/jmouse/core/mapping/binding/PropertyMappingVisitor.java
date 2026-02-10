package org.jmouse.core.mapping.binding;

/**
 * Visitor for evaluating or transforming {@link PropertyMapping} definitions. 🧠
 *
 * <p>{@code PropertyMappingVisitor} is part of the mapping evaluation pipeline. It enables
 * runtime interpretation of mapping definitions without using {@code instanceof} checks,
 * keeping the mapping algebra closed and extensible.</p>
 *
 * <p>Typical responsibilities of visitor implementations:</p>
 * <ul>
 *   <li>resolve final property values</li>
 *   <li>apply decorators (default value, transform, conditional, etc.)</li>
 *   <li>implement evaluation strategies for composite mappings (coalesce, required, ...)</li>
 * </ul>
 *
 * @param <R> visitor result type (e.g. resolved value, execution node, intermediate representation)
 *
 * @see PropertyMapping
 */
public interface PropertyMappingVisitor<R> {

    /**
     * Visit ignore mapping.
     *
     * @param mapping ignore mapping
     * @return visitor result
     */
    R visit(PropertyMapping.Ignore mapping);

    /**
     * Visit constant mapping.
     *
     * @param mapping constant mapping
     * @return visitor result
     */
    R visit(PropertyMapping.Constant mapping);

    /**
     * Visit reference mapping.
     *
     * @param mapping reference mapping
     * @return visitor result
     */
    R visit(PropertyMapping.Reference mapping);

    /**
     * Visit provider mapping.
     *
     * @param mapping provider mapping
     * @return visitor result
     */
    R visit(PropertyMapping.Provider mapping);

    /**
     * Visit compute mapping.
     *
     * @param mapping compute mapping
     * @return visitor result
     */
    R visit(PropertyMapping.Compute mapping);

    /**
     * Visit default value decorator mapping.
     *
     * @param mapping default value mapping
     * @return visitor result
     */
    R visit(PropertyMapping.DefaultValue mapping);

    /**
     * Visit transform decorator mapping.
     *
     * @param mapping transform mapping
     * @return visitor result
     */
    R visit(PropertyMapping.Transform mapping);

    /**
     * Visit conditional mapping.
     *
     * @param mapping conditional mapping
     * @return visitor result
     */
    R visit(PropertyMapping.When mapping);

    /**
     * Visit coalesce composite mapping.
     *
     * @param mapping coalesce mapping
     * @return visitor result
     */
    R visit(PropertyMapping.Coalesce mapping);

    /**
     * Visit required decorator mapping.
     *
     * @param mapping required mapping
     * @return visitor result
     */
    R visit(PropertyMapping.Required mapping);
}
