package org.jmouse.web.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.RequestAttributesHolder;
import org.jmouse.web.http.RequestPath;

/**
 * Base servlet for handling HTTP requests in jMouse framework 🌐
 * <p>
 * Provides lifecycle integration and dispatching logic. Concrete implementations
 * must define initialization and dispatch behavior.
 * </p>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * public class AppDispatcher extends ServletDispatcher {
 *     @Override
 *     protected void doInitialize() {
 *         // initialization logic
 *     }
 *
 *     @Override
 *     protected void doDispatch(HttpServletRequest rq, HttpServletResponse rs, HttpMethod method) throws IOException {
 *         // dispatch logic
 *     }
 *
 *     @Override
 *     protected void doInitialize(WebBeanContext context) {
 *         // optional context-based init
 *     }
 * }
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public abstract class ServletDispatcher extends HttpServlet {

    /**
     * Optional pre-injected web application context.
     */
    protected WebBeanContext context;

    /**
     * Default constructor. Context will be initialized later.
     */
    public ServletDispatcher() {
        this(null);
    }

    /**
     * Constructor with predefined context.
     *
     * @param context WebBeanContext to be used
     */
    public ServletDispatcher(WebBeanContext context) {
        this.context = context;
    }

    /**
     * Servlet initialization entry point.
     */
    @Override
    public void init() throws ServletException {
        doInitialize();
    }

    /**
     * Core HTTP entry point. Stores {@link RequestPath} and delegates to {@code doDispatch}.
     *
     * @param request  the HTTP request
     * @param response the HTTP response
     * @throws IOException in case of I/O failure
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        RequestPath requestPath = (RequestPath) request.getAttribute(RequestPath.REQUEST_PATH_ATTRIBUTE);

        if (requestPath == null) {
            request.setAttribute(RequestPath.REQUEST_PATH_ATTRIBUTE, RequestPath.ofRequest(request));
        }

        doDispatch(request, response);
    }

    /**
     * Lifecycle method called by {@link #init()}. Used to initialize servlet without explicit context.
     */
    protected abstract void doInitialize();

    /**
     * Lifecycle method for manual context-based initialization.
     *
     * @param context injected {@link WebBeanContext}
     */
    protected abstract void doInitialize(WebBeanContext context);

    /**
     * Dispatch method to handle request by method type.
     *
     * @param request     HTTP request
     * @param response    HTTP response
     * @throws IOException in case of I/O failure
     */
    protected abstract void doDispatch(HttpServletRequest request, HttpServletResponse response)
        throws IOException;
}
