package org.jmouse.mvc.adapter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.mvc.*;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.http.HttpStatus;

import java.io.IOException;

/**
 * 🧩 HandlerAdapter for {@link ControllerMethod}.
 *
 * <p>Executes {@link ControllerMethod#handle(HttpServletRequest, HttpServletResponse)}
 * and manages basic response logic.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ControllerMethodHandlerAdapter extends AbstractHandlerAdapter {

    /**
     * 🛠 Handles the request by invoking {@link ControllerMethod}.
     *
     * <ul>
     *   <li>Ensures default content type if not set</li>
     *   <li>Sets {@link HttpStatus} to {@link MvcContainer}</li>
     *   <li>Flushes the response buffer</li>
     * </ul>
     *
     * @param request   current HTTP request
     * @param response  current HTTP response
     * @param handler   the {@link MappedHandler} wrapping {@link ControllerMethod}
     * @param container execution container to collect metadata
     * @return always {@code null}
     * @throws HandlerAdapterException in case of I/O failure
     */
    @Override
    protected Object doHandle(
            HttpServletRequest request, HttpServletResponse response, MappedHandler handler, MvcContainer container) {
        ControllerMethod controller = (ControllerMethod) handler.handler();

        try {
            controller.handle(request, response);
        } catch (IOException e) {
            container.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            throw new HandlerAdapterException("Handler failed during execution.", e);
        }

        return null;
    }

    /**
     * 🚫 No initialization required for {@link ControllerMethodHandlerAdapter}.
     *
     * @param context current web context
     */
    @Override
    protected void doInitialize(WebBeanContext context) {
        // No-op
    }

    /**
     * ✅ Checks if the handler is a {@link ControllerMethod}.
     *
     * @param handler mapped handler
     * @return {@code true} if supported
     */
    @Override
    public boolean supportsHandler(MappedHandler handler) {
        return handler.handler() instanceof ControllerMethod;
    }
}
