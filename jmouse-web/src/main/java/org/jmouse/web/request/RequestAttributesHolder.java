package org.jmouse.web.request;

/**
 * 🔒 Thread-bound holder for {@link RequestAttributes} and {@link RequestPath}.
 *
 * <p>Provides per-thread request context. Automatically propagated to child threads via {@link InheritableThreadLocal}.
 * <p>Used internally in Dispatcher chain, filters, interceptors.
 *
 * <p><b>💡 Example:</b>
 * <pre>{@code
 * // At the start of request
 * RequestAttributesHolder.setRequestAttributes(new MyRequestAttributes(...));
 * RequestAttributesHolder.setRequestPath(parsedRequestPath);
 *
 * // Somewhere deeper
 * String userId = (String) RequestAttributesHolder.getRequestAttributes()
 *         .getAttribute("user.id");
 *
 * // After response commit
 * RequestAttributesHolder.removeRequestAttributes();
 * RequestAttributesHolder.removeRequestPath();
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @since jMouse Web 1.0
 */
public class RequestAttributesHolder {

    private static final ThreadLocal<RequestAttributes> ATTRIBUTES_THREAD_LOCAL = new InheritableThreadLocal<>();
    private static final ThreadLocal<RequestPath> REQUEST_PATH_THREAD_LOCAL = new InheritableThreadLocal<>();

    /**
     * 🧵 Returns the {@link RequestAttributes} bound to the current thread.
     *
     * @return current request attributes, or {@code null} if not set
     */
    public static RequestAttributes getRequestAttributes() {
        return ATTRIBUTES_THREAD_LOCAL.get();
    }

    /**
     * 🧵 Binds the given {@link RequestAttributes} to the current thread.
     *
     * @param attributes the request attributes to set
     */
    public static void setRequestAttributes(RequestAttributes attributes) {
        ATTRIBUTES_THREAD_LOCAL.set(attributes);
    }

    /**
     * ❌ Clears any {@link RequestAttributes} bound to the current thread.
     */
    public static void removeRequestAttributes() {
        ATTRIBUTES_THREAD_LOCAL.remove();
    }

    /**
     * 🔎 Returns the current {@link RequestPath} from thread-local or fallback to {@link RequestAttributes}.
     *
     * @return parsed request path, or {@code null} if not available
     */
    public static RequestPath getRequestPath() {
        RequestPath requestPath = REQUEST_PATH_THREAD_LOCAL.get();

        if (requestPath == null) {
            requestPath = (RequestPath) getRequestAttributes()
                    .getAttribute(RequestPath.REQUEST_PATH_ATTRIBUTE);
        }

        return requestPath;
    }

    /**
     * 🔗 Binds a {@link RequestPath} to the current thread.
     *
     * @param requestPath parsed path info (typically from router)
     */
    public static void setRequestPath(RequestPath requestPath) {
        REQUEST_PATH_THREAD_LOCAL.set(requestPath);
    }

    /**
     * ❌ Clears any {@link RequestPath} bound to the current thread.
     */
    public static void removeRequestPath() {
        REQUEST_PATH_THREAD_LOCAL.remove();
    }
}
