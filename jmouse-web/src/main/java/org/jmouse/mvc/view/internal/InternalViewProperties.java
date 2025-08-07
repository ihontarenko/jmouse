package org.jmouse.mvc.view.internal;

import org.jmouse.context.BeanProperties;
import org.jmouse.mvc.ViewProperties;

/**
 * 🧾 Configuration for internal view rendering.
 *
 * <p>Defines default prefix and suffix used to resolve view templates.
 *
 * <p>Example:
 * <pre>{@code
 * // Resolves view name "home" to: templates/home.j.html
 * }</pre>
 *
 * <p>Configuration prefix: {@code jmouse.view.internal}
 *
 * @author Ivan Hontarenko
 * @author ihontarenko@gmail.com
 */
@BeanProperties(InternalViewProperties.JMOUSE_VIEW_INTERNAL_PATH)
public class InternalViewProperties implements ViewProperties {

    public static final String JMOUSE_VIEW_INTERNAL_PATH = "jmouse.view.internal";

    private String prefix;
    private String suffix;

    /**
     * Gets the view prefix.
     *
     * @return the view path prefix
     */
    @Override
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets the view prefix (default: {@code templates/}).
     *
     * @param prefix directory prefix for view resolution
     */
    @Override
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Gets the view suffix.
     *
     * @return the view file extension
     */
    @Override
    public String getSuffix() {
        return suffix;
    }

    /**
     * Sets the view suffix (default: {@code .j.html}).
     *
     * @param suffix file extension for view resolution
     */
    @Override
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
}
