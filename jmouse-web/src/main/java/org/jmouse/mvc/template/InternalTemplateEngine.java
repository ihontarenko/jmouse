package org.jmouse.mvc.template;

import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.el.renderable.*;
import org.jmouse.el.renderable.loader.ClasspathLoader;
import org.jmouse.el.renderable.loader.TemplateLoader;
import org.jmouse.mvc.AbstractViewResolver;
import org.jmouse.mvc.View;

import java.util.HashMap;
import java.util.Map;

/**
 * 🛠️ Internal template engine-based {@link org.jmouse.mvc.ViewResolver} implementation.
 *
 * <p>This class integrates a custom template engine (based on {@link TemplateEngine})
 * to resolve views located in the classpath. Templates are expected to reside
 * in the {@code /templates/} directory and use the {@code .j.html} extension.</p>
 *
 * <p>Resolved views are cached in memory for performance efficiency.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 * @see InternalView
 * @see TemplateEngine
 * @see Template
 * @see Renderer
 */
public class InternalTemplateEngine extends AbstractViewResolver {

    private final Engine            engine = new TemplateEngine();
    private final Map<String, View> cache  = new HashMap<>();
    private final Renderer          renderer;

    /**
     * Constructs a new {@code InternalTemplateEngine} with default classpath-based loader settings.
     * <p>
     * The loader is configured to read templates from:
     * <pre>
     *     /templates/&lt;viewName&gt;.j.html
     * </pre>
     */
    @BeanConstructor
    public InternalTemplateEngine(InternalViewProperties properties) {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix(properties.getPrefix());
        loader.setSuffix(properties.getSuffix());

        engine.setLoader(loader);
        renderer = new TemplateRenderer(engine);
    }

    /**
     * Resolves a view by its logical path.
     * <p>
     * Caches previously resolved templates for reuse.
     *
     * @param viewPath the logical view path (e.g. "home", "user/profile")
     * @return the resolved {@link View}, or {@code null} if not found
     */
    @Override
    public View resolveView(String viewPath) {
        return cache.computeIfAbsent(viewPath, this::resolveTemplate);
    }

    /**
     * Internal method to load and wrap a template view.
     *
     * @param path the template path
     * @return an {@link InternalView} ready for rendering
     */
    private View resolveTemplate(String path) {
        return new InternalView(engine.getTemplate(path), renderer);
    }
}
