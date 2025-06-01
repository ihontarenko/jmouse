package org.jmouse.web.servlet.filter;

import jakarta.servlet.*;
import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanNameKeeper;
import org.jmouse.web.context.WebBeanContext;

import java.io.IOException;

/**
 * A servlet {@link Filter} that delegates its filter logic to a target filter bean
 * resolved from a {@link BeanContext}.  Useful for late-binding or proxying filters
 * defined in the application context rather than as a concrete class in web.xml.
 * <p>
 * On first invocation of {@link #doFilter}, the filter bean is looked up by name
 * from the associated {@link BeanContext} (or discovered via {@link WebBeanContext})
 * and cached for subsequent calls.  The delegate is initialized with this filter’s
 * {@link FilterConfig} on each request.
 * </p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * <!-- In your servlet registration: -->
 * FilterRegistrationBean<DelegatingBeanFilter> bean =
 *     new FilterRegistrationBean<>(new DelegatingBeanFilter("customFilterBean"));
 * bean.addUrlPatterns("/*");
 * }</pre>
 *
 * @see BeanNameKeeper
 * @see WebBeanContext
 */
public class DelegatingBeanFilter implements Filter, BeanNameKeeper {

    private final Object       monitor = new Object();
    private       FilterConfig delegateConfig;
    private       Filter       delegate;
    private       String       beanName;
    private       BeanContext  beanContext;

    /**
     * Create a new DelegatingBeanFilter that will look up the filter bean with the given name.
     * The {@link BeanContext} will be resolved lazily from {@link WebBeanContext}.
     *
     * @param beanName the name of the target filter bean in the context; must not be {@code null}
     */
    public DelegatingBeanFilter(String beanName) {
        this(beanName, null);
    }

    /**
     * Create a new DelegatingBeanFilter that will use the given {@link BeanContext}
     * to resolve the target filter bean by name.
     *
     * @param beanName    the name of the target filter bean; must not be {@code null}
     * @param beanContext the {@link BeanContext} instance to use for lookup (may be {@code null})
     */
    public DelegatingBeanFilter(String beanName, BeanContext beanContext) {
        this.beanName = beanName;
        this.beanContext = beanContext;
    }

    /**
     * Save the {@link FilterConfig} for later use when initializing the delegate.
     *
     * @param config the filter configuration provided by the servlet container; never {@code null}
     * @throws ServletException if the filter cannot be initialized (ignored here; delegate will be initialized later)
     */
    @Override
    public void init(FilterConfig config) throws ServletException {
        this.delegateConfig = config;
    }

    /**
     * Delegate each request to the target filter bean.
     * <ul>
     *     <li>On first call, resolves the delegate from the {@link BeanContext} (or discovers one via {@link WebBeanContext}).</li>
     *     <li>Initializes the delegate with this filter’s {@link FilterConfig}.</li>
     *     <li>Invokes {@link Filter#doFilter(ServletRequest, ServletResponse, FilterChain)} on the delegate.</li>
     * </ul>
     *
     * @param request  the current HTTP request; never {@code null}
     * @param response the current HTTP response; never {@code null}
     * @param chain    the filter chain to delegate to; never {@code null}
     * @throws IOException      if an I/O error occurs during filter processing
     * @throws ServletException if the delegate filter throws a servlet exception
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                                     ServletException {
        Filter delegate = this.delegate;

        if (delegate == null) {
            synchronized (monitor) {
                delegate = this.delegate;
                if (delegate == null) {
                    delegate = resolveDelegate();
                    this.delegate = delegate;
                    delegate.init(getDelegateConfig());
                }
            }
        }

        delegate.doFilter(request, response, chain);
    }

    /**
     * Look up the target filter bean by name from the configured {@link BeanContext},
     * falling back to a globally registered {@link WebBeanContext} if necessary.
     *
     * @return the resolved filter bean instance; never {@code null}
     * @throws IllegalStateException if no {@link BeanContext} or filter bean can be found
     */
    private Filter resolveDelegate() {
        BeanContext context = getBeanContext();

        if (context == null) {
            context = WebBeanContext.findWebBeanContext(getDelegateConfig().getServletContext());

            if (context == null) {
                throw new IllegalStateException("Unable to find WebBeanContext");
            }

            this.beanContext = context;
        }

        Filter bean = context.getBean(Filter.class, getBeanName());

        if (bean == null) {
            throw new IllegalStateException("No Filter bean named '" + getBeanName() + "' found in context");
        }

        return bean;
    }

    /**
     * Return the {@link FilterConfig} originally provided to this filter.
     *
     * @return the saved filter configuration; may be {@code null} if {@link #init} was never called
     */
    public FilterConfig getDelegateConfig() {
        return delegateConfig;
    }

    /**
     * Return the configured {@link BeanContext}.
     *
     * @return the bean context or {@code null} if not yet set
     */
    public BeanContext getBeanContext() {
        return beanContext;
    }

    /**
     * Set the {@link BeanContext} to use for resolving the delegate filter.
     * <p>If not provided, {@link WebBeanContext} will be used to locate the context at runtime.</p>
     *
     * @param beanContext the bean context to use; may be {@code null}
     */
    public void setBeanContext(BeanContext beanContext) {
        this.beanContext = beanContext;
    }

    /**
     * Return the name of the target filter bean.
     *
     * @return the bean name; never {@code null}
     */
    public String getBeanName() {
        return beanName;
    }

    /**
     * Set the bean name to be used when resolving the delegate filter.
     *
     * @param beanName the bean name in the target {@link BeanContext}; must not be {@code null}
     */
    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    /**
     * Called by the servlet container when the filter is destroyed.  If the delegate
     * has been initialized, calls its {@link Filter#destroy} method.
     */
    @Override
    public void destroy() {
        if (delegate != null) {
            delegate.destroy();
        }
    }
}
