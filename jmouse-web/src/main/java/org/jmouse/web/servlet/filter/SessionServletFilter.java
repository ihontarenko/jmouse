package org.jmouse.web.servlet.filter;

import jakarta.servlet.*;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.BeanScope;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.web.http.request.RequestAttributes;
import org.jmouse.web.http.request.SessionAttributesHolder;

import java.io.IOException;

/**
 * 📶 Servlet {@link Filter} that binds current HTTP request to session-scoped {@link RequestAttributes}.
 *
 * <p>This filter ensures that any component or service accessing {@link SessionAttributesHolder#getRequestAttributes()}
 * receives a session-level {@link RequestAttributes} associated with the current {@link HttpServletRequest}.</p>
 *
 * <h3>✨ When to use</h3>
 * <ul>
 *   <li>When you need to expose session-scoped attributes to beans via a shared holder.</li>
 *   <li>When using {@code @SessionScoped} or working with session-level state manually.</li>
 * </ul>
 *
 * <h3>⚙️ Example integration</h3>
 * <pre>{@code
 * <filter>
 *   <filter-name>sessionFilter</filter-name>
 *   <filter-class>org.jmouse.web.servlet.filter.SessionServletFilter</filter-class>
 * </filter>
 * <filter-mapping>
 *   <filter-name>sessionFilter</filter-name>
 *   <url-pattern>/*</url-pattern>
 * </filter-mapping>
 * }</pre>
 *
 * @see RequestAttributes
 * @see SessionAttributesHolder
 * @see BeanScope#SESSION
 * @see jakarta.servlet.Filter
 * @see HttpServletRequest
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Bean
public class SessionServletFilter implements Filter {

    /**
     * ✨ Binds session-scoped {@link RequestAttributes} before passing request down the chain.
     *
     * @param rq    the incoming request
     * @param rs    the outgoing response
     * @param chain the remaining filter chain
     * @throws IOException      if an I/O error occurs
     * @throws ServletException if a servlet error occurs
     */
    @Override
    public void doFilter(ServletRequest rq, ServletResponse rs, FilterChain chain)
            throws IOException, ServletException {
        if (rq instanceof HttpServletRequest request) {
            // 📦 Store session attributes bound to the current request
            SessionAttributesHolder.setRequestAttributes(
                    RequestAttributes.of(BeanScope.SESSION, request)
            );
        }

        // ➡️ Continue the filter chain
        chain.doFilter(rq, rs);
    }
}
