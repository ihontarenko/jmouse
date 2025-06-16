package org.jmouse.web.context;

import jakarta.servlet.ServletContext;
import org.jmouse.context.AbstractApplicationBeanContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jmouse.beans.BeanContext;

import static org.jmouse.core.reflection.Reflections.getShortName;

/**
 * 🧩 Abstract base implementation of {@link WebBeanContext} that integrates
 * the servlet environment with the jMouse application context.
 * <p>
 * This class bridges the {@link ServletContext} with the DI container by:
 * <ul>
 *     <li>Registering the {@link ServletContext} as a managed bean.</li>
 *     <li>Allowing bean resolution based on servlet context attributes.</li>
 *     <li>Supporting hierarchical context inheritance from a parent {@link BeanContext}.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * AbstractWebApplicationBeanContext context = new MyWebAppContext(parentContext);
 * context.setServletContext(servletContext);
 * }</pre>
 *
 * @see WebBeanContext
 * @see ServletContext
 * @see AbstractApplicationBeanContext
 * @see org.jmouse.beans.BeanContext
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @since 1.0
 */
public class AbstractWebApplicationBeanContext extends AbstractApplicationBeanContext implements WebBeanContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWebApplicationBeanContext.class);

    /**
     * Constructs a new web-aware application context with a given parent context.
     *
     * @param parent the parent {@link BeanContext}; may be {@code null}
     */
    public AbstractWebApplicationBeanContext(BeanContext parent) {
        super(parent);
    }

    /**
     * Constructs a new web-aware application context that scans the specified base classes.
     *
     * @param baseClasses the base classes to scan for components; never {@code null}
     */
    public AbstractWebApplicationBeanContext(Class<?>... baseClasses) {
        super(baseClasses);
    }

    /**
     * Returns the current {@link ServletContext} registered in this bean context.
     *
     * @return the servlet context; may throw if not set
     */
    @Override
    public ServletContext getServletContext() {
        return getBean(ServletContext.class);
    }

    /**
     * Registers the provided {@link ServletContext} as a bean in the context.
     * <p>
     * This enables other components to inject or retrieve the {@link ServletContext}
     * from the DI container.
     * </p>
     *
     * @param servletContext the servlet context to register; must not be {@code null}
     */
    @Override
    public void setServletContext(ServletContext servletContext) {
        LOGGER.info("Attach servlet context '{}'", getShortName(servletContext.getClass()));
        registerBean(ServletContext.class, servletContext);
    }

}
