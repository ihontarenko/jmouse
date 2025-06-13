package org.jmouse.web.mvc.filter;

import org.jmouse.beans.annotation.Provide;
import org.jmouse.util.Priority;
import org.jmouse.web.servlet.DelegatingBeanFilterRegistration;

/**
 * 🔐 Registers the {@link SessionServletFilter} as a delegating filter
 * using {@link DelegatingBeanFilterRegistration}.
 *
 * 📦 This filter enables HTTP session support for MVC controllers and
 * is typically placed early in the filter chain.
 *
 * 🧵 It is automatically registered as a bean via {@link Provide}, and
 * its {@link Priority} ensures it runs before most other filters.
 *
 * ⏱️ Runs with high priority: {@code -2100}
 *
 * @see SessionServletFilter
 * @see DelegatingBeanFilterRegistration
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(-2100)
public class SessionServletFilterRegistration extends DelegatingBeanFilterRegistration {

    /**
     * 🏗️ Registers the {@link SessionServletFilter} using its class.
     * The actual filter logic is delegated through {@link DelegatingBeanFilterRegistration}.
     */
    public SessionServletFilterRegistration() {
        super(SessionServletFilter.class);
    }
}
