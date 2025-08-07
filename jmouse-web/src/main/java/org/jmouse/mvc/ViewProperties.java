package org.jmouse.mvc;

import org.jmouse.core.bind.BindDefault;

/**
 * 🧾 Defines configurable view path settings.
 *
 * <p>Used to apply common prefix/suffix for view name resolution (e.g. resolving "home"
 * to "/WEB-INF/views/home.jsp" via prefix/suffix).
 *
 * @author Ivan Hontarenko
 * @see ViewResolver
 */
public interface ViewProperties {

    String DEFAULT_SUFFIX = ".j.html";
    String DEFAULT_PREFIX = "templates/";

    /**
     * 📍 Returns the configured view prefix.
     */
    String getPrefix();

    /**
     * 🔧 Sets the prefix to prepend to all view names.
     */
    @BindDefault(DEFAULT_PREFIX)
    void setPrefix(String prefix);

    /**
     * 📍 Returns the configured view suffix.
     */
    String getSuffix();

    /**
     * 🔧 Sets the suffix to append to all view names.
     */
    @BindDefault(DEFAULT_SUFFIX)
    void setSuffix(String suffix);
}
