package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.servlet.ServletDispatcher;

/**
 * 🚦 Central dispatcher for the jMouse MVC framework.
 * <p>
 * Integrates {@link HandlerDispatcher} with the servlet lifecycle.
 * Handles incoming HTTP requests and delegates them to the handler subsystem.
 * </p>
 *
 * <p>Typical entry point for the framework via {@code web.xml} or programmatic registration.</p>
 *
 * <pre>{@code
 * <servlet>
 *     <servlet-name>dispatcher</servlet-name>
 *     <servlet-class>org.jmouse.mvc.FrameworkDispatcher</servlet-class>
 * </servlet>
 * }</pre>
 *
 * @see ServletDispatcher
 * @see HandlerDispatcher
 */
public class FrameworkDispatcher extends ServletDispatcher {

    /**
     * 🏷 Default name for the jMouse dispatcher servlet.
     */
    public static final String DEFAULT_DISPATCHER = "jMouseDefaultDispatcher";

    /**
     * 🧭 Core handler dispatcher responsible for request routing and execution.
     */
    private final HandlerDispatcher dispatcher = new HandlerDispatcher();

    /**
     * Creates an uninitialized dispatcher.
     */
    public FrameworkDispatcher() {
    }

    /**
     * Creates a dispatcher with a pre-configured {@link WebBeanContext}.
     *
     * @param context web bean context for dependency injection and configuration
     */
    public FrameworkDispatcher(WebBeanContext context) {
        super(context);
    }

    /**
     * 🔧 Initializes dispatcher from the servlet context if no context was set.
     */
    @Override
    protected void doInitialize() {
        WebBeanContext rootContext = WebBeanContext.getRootWebBeanContext(getServletContext());

        if (context == null) {
            context = rootContext;
        }

        doInitialize(context);
    }

    /**
     * 🔧 Initializes internal {@link HandlerDispatcher} with the given context.
     *
     * @param context the initialized web bean context
     */
    @Override
    protected void doInitialize(WebBeanContext context) {
        dispatcher.initialize(context);
    }

    /**
     * 🚀 Delegates the HTTP request to the internal handler dispatcher.
     *
     * @param request  incoming HTTP servlet request
     * @param response outgoing HTTP servlet response
     */
    @Override
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) {
        dispatcher.dispatch(request, response);
    }
}
