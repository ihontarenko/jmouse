package org.jmouse.web.context;

import org.jmouse.beans.BeanContainer;
import org.jmouse.web.request.RequestAttributes;
import org.jmouse.web.request.RequestAttributesHolder;
import org.jmouse.web.servlet.RequestContextListener;

/**
 * {@link BeanContainer} implementation that stores beans in the current
 * HTTP request scope. Each bean is stored as a request attribute under
 * a unique key to avoid name collisions.
 * <p>
 * <strong>Usage:</strong> To use request‐scoped beans, ensure that
 * {@link RequestContextListener} is registered in your web application.
 * This listener populates {@link RequestAttributes} for each incoming request,
 * allowing this container to bind beans to the lifecycle of that request.
 * </p>
 */
public class RequestScopedBeanContainer implements BeanContainer {

    /**
     * Prefix applied to all bean names to form the actual request attribute key.
     * Prevents collisions with other request attributes.
     */
    public static final String BEAN_NAMES_PREFIX = RequestScopedBeanContainer.class.getName() + ".BEAN:";

    /**
     * Retrieve a bean from the current request scope by name.
     *
     * @param name the logical bean name (never {@code null})
     * @param <T>  the expected bean type
     * @return the bean instance bound to the current request, or {@code null}
     * if no bean is registered under that name
     * @throws WebContextException if there is no active request or the
     *                             {@link RequestContextListener} is not registered
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        return (T) getRequestAttributes().getAttribute(getResolvedName(name));
    }

    /**
     * Register a bean object under the given name in the current request scope.
     * If no session exists, a {@link WebContextException} is thrown.
     *
     * @param name the logical bean name (never {@code null})
     * @param bean the bean instance to bind to the request (never {@code null})
     * @throws WebContextException if there is no active request or the
     *                             {@link RequestContextListener} is not registered
     */
    @Override
    public void registerBean(String name, Object bean) {
        getRequestAttributes().setAttribute(getResolvedName(name), bean);
    }

    /**
     * Determine whether a bean with the given name exists in the current request scope.
     *
     * @param name the logical bean name (never {@code null})
     * @return {@code true} if a bean is registered under that name; {@code false} otherwise
     * @throws WebContextException if there is no active request or the
     *                             {@link RequestContextListener} is not registered
     */
    @Override
    public boolean containsBean(String name) {
        return getBean(name) != null;
    }

    /**
     * Compute the actual request attribute key for a given bean name
     * by prepending the {@link #BEAN_NAMES_PREFIX} constant.
     *
     * @param name the logical bean name (never {@code null})
     * @return the resolved key used to store the bean in {@link RequestAttributes}
     */
    public String getResolvedName(String name) {
        return BEAN_NAMES_PREFIX + name;
    }

    /**
     * Obtain the current {@link RequestAttributes} for this thread,
     * which was initialized by {@link RequestContextListener}.
     *
     * @return the {@link RequestAttributes} associated with the active request
     * @throws WebContextException if there is no active request or the
     *                             {@link RequestContextListener} is not registered
     */
    public RequestAttributes getRequestAttributes() {
        RequestAttributes attributes = RequestAttributesHolder.getRequestAttributes();

        if (attributes == null) {
            throw new WebContextException(
                    "Unsupported bean-scope REQUEST. Is '%s' listener registered or no active request?".formatted(
                            RequestContextListener.class.getName()));
        }

        return attributes;
    }
}
