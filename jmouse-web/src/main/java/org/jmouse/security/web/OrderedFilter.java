package org.jmouse.security.web;

import jakarta.servlet.*;
import org.jmouse.core.Ordered;

import java.io.IOException;

/**
 * 📐 OrderedFilter
 *
 * Decorator that associates a {@link Filter} with an explicit order value.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>🔗 Delegate {@link #doFilter} directly to the wrapped filter</li>
 *   <li>📊 Expose an {@code order} for sorting and predictable chain execution</li>
 *   <li>📝 Provide a descriptive {@link #toString()} for debugging/logging</li>
 * </ul>
 *
 * <p>💡 Useful when multiple security or servlet filters need to be applied in a
 * specific order within a {@link FilterChain}.</p>
 */
public record OrderedFilter(Filter filter, int order) implements Filter, Ordered {

    /**
     * 🚦 Delegates request/response handling to the wrapped filter.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        filter.doFilter(request, response, chain);
    }

    /**
     * 📝 String representation showing order and filter type.
     *
     * @return formatted description
     */
    @Override
    public String toString() {
        return "OrderedFilter[%s][%s]".formatted(order, filter.getClass().getSimpleName());
    }
}
