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
     * Renders a template using the provided data model.
     *
     * <p>Equivalent to calling {@link #render(String, Object, UnaryOperator)}
     * with an identity request customizer.</p>
     *
     * @param templateKey logical template identifier
     * @param data        data model object
     * @return rendered result
     */
    default T render(String templateKey, Object data) {
        return render(templateKey, data, request -> request);
    }

    /**
     * Renders a template with request customization.
     *
     * @param templateKey        logical template identifier
     * @param data               data model object
     * @param requestCustomizer  customization callback applied before rendering
     * @return rendered result
     */
    T render(String templateKey, Object data, UnaryOperator<RenderingRequest> requestCustomizer);

}
