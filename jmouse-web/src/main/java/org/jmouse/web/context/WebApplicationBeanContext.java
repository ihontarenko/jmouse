package org.jmouse.web.context;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanScope;
import org.jmouse.beans.container.ThreadLocalBeanContainer;
import org.jmouse.beans.container.ThreadLocalScope;

/**
 * 🌐 Specialized bean context for web environments.
 * <p>
 * Automatically registers request, session, and thread-local scopes.
 * </p>
 *
 * <pre>{@code
 * WebApplicationBeanContext context = new WebApplicationBeanContext(WebConfig.class);
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @see RequestAttributesBeanContainer
 * @see ThreadLocalBeanContainer
 */
public class WebApplicationBeanContext extends AbstractWebApplicationBeanContext {

    /**
     * Creates a web application context with a parent and base classes.
     *
     * @param parent      optional parent context (can be {@code null})
     * @param baseClasses packages to scan for beans
     */
    public WebApplicationBeanContext(BeanContext parent, Class<?>... baseClasses) {
        super(parent);

        addBaseClasses(baseClasses);

        registerBeanContainer(ThreadLocalScope.THREAD_LOCAL_SCOPE, new ThreadLocalBeanContainer());
        registerBeanContainer(BeanScope.REQUEST, new RequestAttributesBeanContainer(BeanScope.REQUEST));
        registerBeanContainer(BeanScope.SESSION, new RequestAttributesBeanContainer(BeanScope.SESSION));
    }

    /**
     * Creates a sourceRoot web context without a parent.
     *
     * @param baseClasses packages to scan for beans
     */
    public WebApplicationBeanContext(Class<?>... baseClasses) {
        this(null, baseClasses);
    }

}
