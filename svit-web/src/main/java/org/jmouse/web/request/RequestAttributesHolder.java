package org.jmouse.web.request;

/**
 * A holder class that stores {@link RequestAttributes} in a thread-local variable.
 * Useful for associating request or session attributes with the current execution thread.
 *
 * <p>Example usage (in a filter or interceptor):
 * <pre>{@code
 * // When a request arrives
 * RequestAttributesHolder.setRequestAttributes(new MyRequestAttributesImpl(...));
 *
 * // Later in the same thread
 * RequestAttributes attributes = RequestAttributesHolder.getRequestAttributes();
 * Object user = attributes.getAttribute("user");
 *
 * // When the request is completed
 * RequestAttributesHolder.clearRequestAttributes();
 * }</pre>
 */
public class RequestAttributesHolder {

    private static final ThreadLocal<RequestAttributes> ATTRIBUTES_THREAD_LOCAL = new InheritableThreadLocal<>();

    /**
     * Retrieves the current {@link RequestAttributes} from the thread-local storage.
     *
     * @return the current request attributes, or {@code null} if none are set.
     */
    public static RequestAttributes getRequestAttributes() {
        return ATTRIBUTES_THREAD_LOCAL.get();
    }

    /**
     * Sets the current {@link RequestAttributes} in the thread-local storage.
     *
     * @param attributes the request attributes to associate with the current thread.
     */
    public static void setRequestAttributes(RequestAttributes attributes) {
        ATTRIBUTES_THREAD_LOCAL.set(attributes);
    }

    /**
     * Removes any {@link RequestAttributes} associated with the current thread.
     */
    public static void clearRequestAttributes() {
        ATTRIBUTES_THREAD_LOCAL.remove();
    }
}
