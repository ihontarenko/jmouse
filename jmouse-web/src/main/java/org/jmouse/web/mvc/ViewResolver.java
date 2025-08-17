package org.jmouse.web.mvc;

/**
 * 🧩 Strategy interface for resolving a {@link View} by its logical path.
 *
 * <p>Used by the MVC framework to translate a view name (e.g. {@code "user/profile"})
 * into a concrete {@link View} implementation (e.g. HTML, JSON, view engine).
 *
 * <p>Custom implementations may support prefix/suffix mapping, localization,
 * view caching, or dynamic resolution.
 *
 * <pre>{@code
 * ViewResolver resolver = new TemplateViewResolver();
 * View view = resolver.resolveView("home");
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ViewResolver {

    /**
     * 🎯 Resolve a {@link View} based on the given logical path.
     *
     * @param viewPath the logical view path (e.g. {@code "user/profile"})
     * @return the resolved {@code View}, or {@code null} if not found
     */
    View resolveView(String viewPath);
}
