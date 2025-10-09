package org.jmouse.web.http;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 💥 Thread-local holder for exceptions during request processing.
 *
 * <p>Stores the exception thrown in the current thread to allow
 * later retrieval, typically used during web request dispatching
 * to handle errors gracefully.
 *
 * @author Ivan Hontarenko
 * @author ihontarenko@gmail.com
 */
public class ExceptionHolder {

    public static final String CURRENT_EXCEPTION_ATTRIBUTE = Exception.class.getName() + ".CURRENT";

    private static final ThreadLocal<Exception> EXCEPTION_THREAD_LOCAL = new InheritableThreadLocal<>();

    /**
     * 🔍 Returns the exception stored in the current thread.
     *
     * @return the exception or {@code null} if none is set
     */
    public static Exception getException() {
        return EXCEPTION_THREAD_LOCAL.get();
    }

    /**
     * 💾 Stores the given exception in the current thread.
     *
     * @param exception the exception to store
     */
    public static void setException(Exception exception) {
        EXCEPTION_THREAD_LOCAL.set(exception);
    }

    /**
     * 🧹 Removes the stored exception from the current thread.
     */
    public static void removeException() {
        EXCEPTION_THREAD_LOCAL.remove();
    }

    /**
     * 🔍 Retrieves the current exception from the request attribute or thread-local storage.
     *
     * <p>First tries to get the exception from the request attribute identified by
     * {@code CURRENT_EXCEPTION_ATTRIBUTE}. If not found, falls back to the thread-local storage.
     *
     * @param request the current HTTP servlet request
     * @return the exception associated with the request or thread, or {@code null} if none
     */
    public static Exception getException(HttpServletRequest request) {
        Exception exception = (Exception) request.getAttribute(CURRENT_EXCEPTION_ATTRIBUTE);

        if (exception == null) {
            exception = getException();
        }

        return exception;
    }

    /**
     * 💾 Stores the exception as a request attribute.
     *
     * @param request   the current HTTP servlet request
     * @param exception the exception to store
     */
    public static void setException(HttpServletRequest request, Exception exception) {
        request.setAttribute(CURRENT_EXCEPTION_ATTRIBUTE, exception);
    }

    /**
     * 🧹 Removes the exception attribute from the request.
     *
     * @param request the current HTTP servlet request
     */
    public static void removeException(HttpServletRequest request) {
        request.removeAttribute(CURRENT_EXCEPTION_ATTRIBUTE);
    }


}
