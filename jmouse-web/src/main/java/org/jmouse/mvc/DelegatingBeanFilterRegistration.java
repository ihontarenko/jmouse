package org.jmouse.mvc;

import jakarta.servlet.Filter;
import org.jmouse.beans.annotation.Ignore;
import org.jmouse.util.Priority;
import org.jmouse.web.servlet.DelegatingBeanFilter;
import org.jmouse.web.servlet.registration.FilterRegistrationBean;

/**
 * 🎛️ Special {@link FilterRegistrationBean} for registering a {@link DelegatingBeanFilter}
 * with a canonical, prefixed name and enabling it by default.
 *
 * 📦 This class allows dynamic delegation of filtering logic to a named filter bean
 * that is managed by the application context (via {@link DelegatingBeanFilter}).
 *
 * 📛 The provided filter name is transformed to the format {@code delegate:Name},
 * where the first character is capitalized for consistency.
 *
 * 🧩 This bean is marked with {@link Ignore} to exclude it from component scanning,
 * and annotated with a low {@link Priority} to allow early execution in the filter chain.
 *
 * <h3>📌 Example</h3>
 * <pre>{@code
 * new DelegatingBeanFilterRegistration("authFilter");
 * // Registers as: delegate:AuthFilter
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Ignore
@Priority(-1900)
public class DelegatingBeanFilterRegistration extends FilterRegistrationBean<DelegatingBeanFilter> {

    /**
     * 🏗️ Constructs a new delegating filter registration by name.
     *
     * @param name the name of the target filter bean in the context
     */
    public DelegatingBeanFilterRegistration(String name) {
        super(getDelegatingFilterName(name), new DelegatingBeanFilter(name));
        setEnabled(true);
    }

    /**
     * 🏗️ Constructs a new delegating filter registration by type.
     *
     * @param type the class of the filter bean in the context
     */
    public DelegatingBeanFilterRegistration(Class<? extends Filter> type) {
        super(getDelegatingFilterName(type.getName()), new DelegatingBeanFilter(type));
        setEnabled(true);
    }

    /**
     * 🧮 Transforms a raw filter name into the canonical format used for delegation.
     *
     * <p>Example: {@code "auth"} → {@code "delegate:Auth"}</p>
     *
     * @param name the original name of the filter
     * @return the transformed filter name
     */
    public static String getDelegatingFilterName(String name) {
        return "delegate:" + name;
    }
}
