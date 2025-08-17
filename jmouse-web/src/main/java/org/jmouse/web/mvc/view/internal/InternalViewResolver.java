package org.jmouse.web.mvc.view.internal;

import org.jmouse.beans.annotation.AggregatedBeans;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.el.extension.Extension;
import org.jmouse.el.renderable.*;
import org.jmouse.el.renderable.loader.ClasspathLoader;
import org.jmouse.el.renderable.loader.TemplateLoader;
import org.jmouse.web.mvc.View;
import org.jmouse.web.mvc.ViewProperties;
import org.jmouse.web.mvc.view.AbstractViewResolver;
import org.jmouse.web.mvc.ViewResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🛠️ Internal view engine-based {@link ViewResolver} implementation.
 *
 * <p>Resolves views using the {@link TemplateEngine} and {@link TemplateRenderer}
 * with templates loaded from the classpath.</p>
 *
 * 📦 Templates are expected in:
 * <pre>
 *     /templates/{viewName}.j.html
 * </pre>
 *
 * ⚡ Views are cached after first resolution for fast reuse.
 *
 * 📌 Example:
 * <pre>{@code
 * // Resolves view "/templates/home.j.html"
 * View view = viewResolver.resolveView("home");
 * }</pre>
 *
 * @author Ivan Hontarenko
 * @see TemplateEngine
 * @see InternalView
 * @see Renderer
 */
public class InternalViewResolver extends AbstractViewResolver {

    private final Engine engine = new TemplateEngine();
    private final Map<String, View> cache = new HashMap<>();
    private final Renderer renderer;

    /**
     * 🧱 Constructs a view resolver with a classpath loader and optional extensions.
     *
     * @param properties  configuration for prefix/suffix (e.g. "/templates/", ".j.html")
     * @param extensions  optional expression language extensions
     */
    @BeanConstructor
    public InternalViewResolver(ViewProperties properties, @AggregatedBeans List<Extension> extensions) {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix(properties.getPrefix());
        loader.setSuffix(properties.getSuffix());

        engine.setLoader(loader);
        renderer = new TemplateRenderer(engine);

        if (extensions != null) {
            extensions.forEach(engine.getExtensions()::importExtension);
        }
    }

    /**
     * 🧪 Exposes the underlying {@link Engine}.
     *
     * @return the configured {@link TemplateEngine}
     */
    public Engine getEngine() {
        return engine;
    }

    /**
     * 🗂️ Exposes the internal view cache.
     *
     * @return cached views map
     */
    public Map<String, View> getCache() {
        return cache;
    }

    /**
     * 🎨 Returns the renderer used to evaluate views.
     *
     * @return renderer instance
     */
    public Renderer getRenderer() {
        return renderer;
    }

    /**
     * 🔍 Resolves a logical view name into a {@link View}, using caching.
     *
     * @param viewPath logical view name (e.g. "dashboard", "user/profile")
     * @return resolved view instance
     */
    @Override
    public View resolveView(String viewPath) {
        return cache.computeIfAbsent(viewPath, this::resolveTemplate);
    }

    /**
     * 📥 Loads and wraps the template from the engine.
     *
     * @param path the view path (without prefix/suffix)
     * @return new {@link InternalView} instance
     */
    private View resolveTemplate(String path) {
        return new InternalView(engine.getTemplate(path), renderer);
    }
}
