package org.jmouse.dom.renderer;

/**
 * Factory methods for commonly used {@link RendererRegistry} configurations. 🧰
 *
 * <p>
 * Provides preconfigured renderer registries for standard HTML and XML rendering scenarios.
 * The order of renderers is important — more specific renderers are registered before
 * more generic ones.
 * </p>
 *
 * <h3>Provided configurations</h3>
 * <ul>
 *     <li>{@link #html()} — HTML-oriented registry</li>
 *     <li>{@link #xml()} — XML-oriented registry (includes CDATA support)</li>
 * </ul>
 *
 * <p>
 * The returned registries are immutable.
 * </p>
 */
final public class Renderers {

    private Renderers() {}

    /**
     * Creates a renderer registry configured for HTML output.
     *
     * <p>
     * Includes:
     * </p>
     * <ul>
     *     <li>{@link CommentRenderer}</li>
     *     <li>{@link TextRenderer}</li>
     *     <li>{@link ElementRenderer}</li>
     * </ul>
     *
     * <p>
     * CDATA rendering is not included, as it is typically XML-specific.
     * </p>
     *
     * @return HTML renderer registry
     */
    public static RendererRegistry html() {
        return RendererRegistry.builder()
                .add(new CommentRenderer())
                .add(new TextRenderer())
                .add(new ElementRenderer())
                .build();
    }

    /**
     * Creates a renderer registry configured for XML output.
     *
     * <p>
     * Includes:
     * </p>
     * <ul>
     *     <li>{@link CommentRenderer}</li>
     *     <li>{@link CDataRenderer}</li>
     *     <li>{@link TextRenderer}</li>
     *     <li>{@link ElementRenderer}</li>
     * </ul>
     *
     * <p>
     * CDATA rendering is enabled only in this configuration.
     * </p>
     *
     * @return XML renderer registry
     */
    public static RendererRegistry xml() {
        return RendererRegistry.builder()
                .add(new CommentRenderer())
                .add(new CDataRenderer())
                .add(new TextRenderer())
                .add(new ElementRenderer())
                .build();
    }
}