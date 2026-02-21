package org.jmouse.meterializer;

import java.util.function.UnaryOperator;

/**
 * Contract for template rendering. 🎨
 *
 * <p>
 * Defines a minimal rendering entry point that takes:
 * </p>
 * <ul>
 *     <li>a template identifier</li>
 *     <li>a data model object</li>
 *     <li>an optional {@link RenderingRequest} customizer</li>
 * </ul>
 *
 * <p>
 * The concrete implementation is responsible for resolving the template,
 * preparing the rendering context, evaluating expressions, and producing
 * a result of type {@code T} (e.g. {@code String}, DOM node, byte[]).
 * </p>
 *
 * <h3>Basic usage</h3>
 *
 * <pre>{@code
 * Rendering<String> rendering = ...;
 *
 * String html = rendering.render("user/profile", userDto);
 * }</pre>
 *
 * <h3>Customizing the request</h3>
 *
 * <pre>{@code
 * String html = rendering.render("user/profile", userDto, request ->
 *     request.attribute("locale", "en_US")
 *            .attribute("theme", "dark")
 * );
 * }</pre>
 *
 * <p>
 * Implementations should be thread-safe unless documented otherwise.
 * The {@link RenderingRequest} is expected to be per-invocation.
 * </p>
 *
 * @param <T> rendering result type
 */
public interface Rendering<T> {

    /**
     * Renders a template with request customization.
     *
     * @param templateKey       logical template identifier
     * @param data              data model object
     * @param requestCustomizer customization callback applied before rendering
     * @return rendered result
     */
    T render(String templateKey, Object data, UnaryOperator<RenderingRequest> requestCustomizer);

    /**
     * Renders a template using a {@link ModelReference}.
     *
     * <p>
     * Convenience overload that extracts template key and model
     * from the given reference and delegates to
     * {@link #render(String, Object, UnaryOperator)}.
     * </p>
     *
     * <pre>{@code
     * ModelReference ref = ModelReference.of("user/profile", userDto);
     *
     * T result = rendering.render(ref, request ->
     *     request.attribute("locale", "en_US")
     * );
     * }</pre>
     *
     * @param reference         template reference containing key and model
     * @param requestCustomizer customization callback applied before rendering
     * @return rendered result
     */
    default T render(ModelReference reference, UnaryOperator<RenderingRequest> requestCustomizer) {
        return render(reference.templateReference(), reference.model(), requestCustomizer);
    }

    /**
     * Renders a template using the provided data model.
     *
     * <p>
     * Equivalent to calling {@link #render(String, Object, UnaryOperator)}
     * with an identity request customizer.
     * </p>
     *
     * @param templateReference logical template identifier
     * @param data        data model object
     * @return rendered result
     */
    default T render(String templateReference, Object data) {
        return render(ModelReference.of(templateReference, data), request -> request);
    }

    /**
     * Renders a template using the provided {@link ModelReference}.
     *
     * <p>
     * Equivalent to calling {@link #render(ModelReference, UnaryOperator)}
     * with an identity request customizer.
     * </p>
     *
     * <pre>{@code
     * ModelReference ref = ModelReference.of("user/profile", userDto);
     *
     * T result = rendering.render(ref);
     * }</pre>
     *
     * @param reference template reference containing key and model
     * @return rendered result
     */
    default T render(ModelReference reference) {
        return render(reference, request -> request);
    }

}
