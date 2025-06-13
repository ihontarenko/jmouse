package org.jmouse.web.request;

/**
 * A holder class that stores {@link RequestAttributes} representing session attributes
 * in a thread-local variable.
 *
 * <p>This is useful for associating session-level data with the current execution thread,
 * such as during request handling in web frameworks or filters where session context is needed.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class SessionAttributesHolder {

    private static final ThreadLocal<RequestAttributes> ATTRIBUTES_THREAD_LOCAL = new InheritableThreadLocal<>();

    /**
     * Retrieves the current session {@link RequestAttributes} from thread-local storage.
     *
     * @return the current session attributes, or {@code null} if none are set
     */
    public static RequestAttributes getRequestAttributes() {
        return ATTRIBUTES_THREAD_LOCAL.get();
    }

    /**
     * Retrieves the current {@link ServletSession} instance from thread-local storage.
     *
     * <p>This is a convenience method that casts the internally stored {@link RequestAttributes}
     * to a {@link ServletSession}. It assumes that the stored instance is of the correct type.
     *
     * <p><strong>Note:</strong> If the stored attributes are not an instance of {@code ServletSession},
     * this method will throw a {@link ClassCastException}.
     *
     * @return the current {@link ServletSession} instance, or {@code null} if none is set
     * @throws ClassCastException if the stored attributes are not of type {@link ServletSession}
     */
    public static ServletSession getSession() {
        ServletSession session = null;

        if (ATTRIBUTES_THREAD_LOCAL.get() instanceof ServletSession servletSession) {
            session = servletSession;
        } else {
            RequestAttributes attributes = getRequestAttributes();
            if (attributes.getAttribute(ServletSession.SERVLET_SESSION_ATTRIBUTE) instanceof ServletSession servletSession) {
                session = servletSession;
            }
        }

        return session;
    }

    /**
     * Sets the given {@link RequestAttributes} instance into thread-local storage
     * to be associated with the current thread as session attributes.
     *
     * @param attributes the session attributes to bind to the current thread
     */
    public static void setRequestAttributes(RequestAttributes attributes) {
        ATTRIBUTES_THREAD_LOCAL.set(attributes);
        if (attributes instanceof ServletSession servletSession) {
            attributes.setAttribute(ServletSession.SERVLET_SESSION_ATTRIBUTE, servletSession);
        }
    }

    /**
     * Clears any {@link RequestAttributes} instance currently bound to the thread,
     * effectively removing the session context for the thread.
     */
    public static void clearRequestAttributes() {
        ATTRIBUTES_THREAD_LOCAL.remove();
    }
}
