package org.jmouse.meterializer;

/**
 * Simple holder linking a template reference with a model object. 🔗
 *
 * <p>
 * {@code ModelReference} is typically used when a rendering operation
 * needs to explicitly pass both:
 * </p>
 * <ul>
 *     <li>a template key (logical identifier)</li>
 *     <li>a model object bound as root for that template</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * ModelReference ref = ModelReference.of("user/profile", userDto);
 *
 * rendering.render(ref.templateReference(), ref.model());
 * }</pre>
 *
 * <p>
 * This record is immutable and suitable for passing across rendering layers.
 * </p>
 *
 * @param templateReference logical template identifier
 * @param model root model object associated with the template
 */
public record ModelReference(String templateReference, Object model) {

    /**
     * Factory method for creating a {@link ModelReference}.
     *
     * @param templateReference logical template identifier
     * @param model root model object
     * @return new model reference instance
     */
    public static ModelReference of(String templateReference, Object model) {
        return new ModelReference(templateReference, model);
    }

}