package org.jmouse.web.servlet;

import jakarta.servlet.*;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanNameKeeper;
import org.jmouse.beans.BeanNotFoundException;
import org.jmouse.web.context.WebBeanContext;

import java.io.IOException;

/**
 * 💡 A servlet {@link Filter} that delegates its behavior to a target filter bean
 * resolved from a {@link BeanContext}.
 *
 * 📦 Useful when filters are defined as beans in the application context and should be applied
 * at runtime instead of being declared statically in {@code web.xml}.
 *
 * 🔁 On the first proxyInvocation of {@link #doFilter}, this wrapper locates the filter bean using its
 * name or type, initializes it with the {@link FilterConfig}, and caches it for reuse.
 *
 * <h3>⚙️ Example usage</h3>
 * <pre>{@code
 * FilterRegistrationBean<DelegatingBeanFilter> bean =
 *     new FilterRegistrationBean<>(new DelegatingBeanFilter("authFilter"));
 * bean.addUrlPatterns("/*");
 * }</pre>
 *
 * @see BeanNameKeeper
 * @see WebBeanContext
 */
public class DelegatingBeanFilter implements Filter, BeanNameKeeper {

    private final Object monitor = new Object();

    private FilterConfig            delegateConfig;
    private Filter                  delegate;
    private String                  beanName;
    private Class<? extends Filter> beanType;
    private BeanContext             beanContext;

    /**
     * 🏗️ Constructs a delegating filter using a bean name.
     * The {@link BeanContext} will be lazily resolved via {@link WebBeanContext}.
     *
     * @param beanName the name of the target filter bean in the context; must not be {@code null}
     */
    public DelegatingBeanFilter(String beanName) {
        this(beanName, null, null);
    }

    /**
     * 🏗️ Constructs a delegating filter using a bean type.
     *
     * @param beanType the type of the filter bean to resolve
     */
    public DelegatingBeanFilter(Class<? extends Filter> beanType) {
        this(null, beanType, null);
    }

    /**
     * 🏗️ Constructs a delegating filter using a bean name and optional {@link BeanContext}.
     *
     * @param beanName    the name of the target filter bean
     * @param beanContext the bean context to use for lookup (nullable)
     */
    public DelegatingBeanFilter(String beanName, Class<? extends Filter> beanType, BeanContext beanContext) {
        this.beanName = beanName;
        this.beanType = beanType;
        this.beanContext = beanContext;
    }

    /**
     * 🔧 Stores the provided {@link FilterConfig} for later use when initializing the delegate.
     *
     * @param config the servlet container's filter configuration
     */
    @Override
    public void init(FilterConfig config) throws ServletException {
        this.delegateConfig = config;
    }

    /**
     * 🔁 Delegates filtering to the resolved filter bean.
     * <ul>
     *     <li>🧵 Thread-safe lazy initialization on first call</li>
     *     <li>🔍 Resolves and initializes the delegate via {@link BeanContext}</li>
     *     <li>➡️ Passes control to the delegate's {@link Filter#doFilter} method</li>
     * </ul>
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        Filter delegate = this.delegate;

        if (delegate == null) {
            synchronized (monitor) {
                delegate = this.delegate;
                if (delegate == null) {
                    delegate = getDelegateFilter();
                    this.delegate = delegate;
                    delegate.init(getDelegateConfig());
                }
            }
        }

        delegate.doFilter(request, response, chain);
    }

    /**
     * 🔍 Resolves the delegate filter from the current {@link BeanContext}.
     *
     * @return the resolved filter instance
     * @throws IllegalStateException if no suitable bean is found
     */
    private Filter getDelegateFilter() {
        BeanContext context = getBeanContext();

        try {
            if (getBeanName() != null) {
                return context.getBean(Filter.class, getBeanName());
            }

            if (getBeanType() != null) {
                return context.getBean(getBeanType());
            }

            throw new IllegalStateException("Unable to resolve filter-bean. Specify bean-type or bean-name.");

        } catch (BeanNotFoundException bnf) {
            throw new IllegalStateException(
                    "No filter-bean found '%s' in '%s' context: %s"
                            .formatted(getBeanName(), context.getContextId(), bnf.getMessage()));
        }
    }

    /**
     * 💾 Returns the stored {@link FilterConfig}.
     *
     * @return the configuration object, or {@code null} if {@link #init} was never called
     */
    public FilterConfig getDelegateConfig() {
        return delegateConfig;
    }

    /**
     * 🌐 Resolves the {@link BeanContext} either from the field or via {@link WebBeanContext}.
     *
     * @return the resolved bean context
     * @throws IllegalStateException if no context is available
     */
    public BeanContext getBeanContext() {
        BeanContext context = this.beanContext;

        if (context == null) {
            ServletContext sc = getDelegateConfig().getServletContext();
            context = WebBeanContext.findWebBeanContext(sc);

            if (context == null) {
                throw new IllegalStateException("Unable to find WebBeanContext");
            }

            this.beanContext = context;
        }

        return beanContext;
    }

    /**
     * 🔧 Sets a specific {@link BeanContext} to use for bean lookup.
     *
     * @param beanContext the context to set (nullable)
     */
    public void setBeanContext(BeanContext beanContext) {
        this.beanContext = beanContext;
    }

    /**
     * 🔍 Returns the type of the filter bean to be resolved.
     *
     * @return the filter bean type, or {@code null}
     */
    public Class<? extends Filter> getBeanType() {
        return beanType;
    }

    /**
     * 🔧 Sets the filter bean type to use during resolution.
     *
     * @param beanType the class of the filter to resolve
     */
    public void setBeanType(Class<? extends Filter> beanType) {
        this.beanType = beanType;
    }

    /**
     * 🔍 Returns the name of the filter bean to be resolved.
     *
     * @return the filter bean name
     */
    public String getBeanName() {
        return beanName;
    }

    /**
     * 🔧 Sets the filter bean name to use during resolution.
     *
     * @param beanName the name of the filter bean
     */
    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    /**
     * 🧹 Called by the servlet container on shutdown. Destroys the delegate if initialized.
     */
    @Override
    public void destroy() {
        if (delegate != null) {
            delegate.destroy();
        }
    }
}
