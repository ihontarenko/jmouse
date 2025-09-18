package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * ⚙️ Encapsulates the execution context for a matched handler, including
 * a list of {@link HandlerInterceptor interceptors} and the handler itself.
 *
 * <p>This class is responsible for invoking interceptor chains before and
 * after the actual handler execution.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * Handler execution = new Handler(handler);
 * execution.addInterceptor(new LoggingInterceptor());
 * if (execution.preHandle(request, response)) {
 *     MVCResult result = adapter.handle(request, response, handler);
 *     execution.postHandle(request, response, result);
 * }
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @see HandlerInterceptor
 */
public final class Handler {

    private final List<HandlerInterceptor> interceptors = new ArrayList<>();
    private       MappedHandler            handler;

    /**
     * Constructs a new {@code HandlerContainer} with the specified handler.
     *
     * @param handler the resolved handler object for the request
     */
    public Handler(MappedHandler handler) {
        this.handler = handler;
    }

    /**
     * Returns an unmodifiable list of registered {@link HandlerInterceptor}s.
     *
     * @return the list of interceptors
     */
    public List<HandlerInterceptor> getInterceptors() {
        return List.copyOf(interceptors);
    }

    /**
     * Registers an additional {@link HandlerInterceptor} in the execution chain.
     *
     * @param interceptor the interceptor to add
     */
    public void addInterceptor(HandlerInterceptor interceptor) {
        this.interceptors.add(interceptor);
    }

    /**
     * Inserts a {@link HandlerInterceptor} at the given position in the execution chain.
     *
     * <p>Index is zero-based. Existing elements at and after {@code position} are shifted to the right.
     * The final order determines interceptor execution order in the chain.</p>
     *
     * @param position   zero-based insertion index (from {@code 0} to current size)
     * @param interceptor the interceptor to insert
     * @throws IndexOutOfBoundsException if {@code position} is out of range ({@code position < 0 || position > size()})
     * @see HandlerInterceptor
     */
    public void addInterceptor(int position, HandlerInterceptor interceptor) {
        this.interceptors.add(position, interceptor);
    }

    /**
     * Returns the current handler associated with this execution context.
     *
     * @return the handler object
     */
    public MappedHandler getHandler() {
        return handler;
    }

    /**
     * Replaces the current handler.
     *
     * @param handler the new handler object
     */
    public void setHandler(MappedHandler handler) {
        this.handler = handler;
    }

    /**
     * Executes all {@link HandlerInterceptor#preHandle} methods in order.
     *
     * @param request  the current HTTP request
     * @param response the current HTTP response
     * @return {@code true} if all interceptors returned {@code true}; {@code false} if processing should halt
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response) {
        boolean successful = true;

        for (HandlerInterceptor interceptor : getInterceptors()) {
            if (!interceptor.preHandle(request, response, getHandler())) {
                successful = false;
                break;
            }
        }

        return successful;
    }

    /**
     * Executes all {@link HandlerInterceptor#postHandle} methods in order.
     *
     * @param request  the current HTTP request
     * @param response the current HTTP response
     * @param result   the handler's response
     */
    public void postHandle(HttpServletRequest request, HttpServletResponse response, MVCResult result) {
        for (HandlerInterceptor interceptor : getInterceptors()) {
            interceptor.postHandle(request, response, getHandler(), result);
        }
    }
}
