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
     *   <li>Sets {@link HttpStatus} to {@link InvocationOutcome}</li>
     *   <li>Flushes the response buffer</li>
     * </ul>
     *
     * @throws HandlerAdapterException in case of I/O failure
     */
    @Override
    protected void doHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            MappedHandler mappedHandler,
            InvocationOutcome result
    ) {
        try {
            ControllerMethod controller = (ControllerMethod) mappedHandler.handler();
            controller.handle(request, response);
        } catch (IOException e) {
            result.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            throw new HandlerAdapterException("Handler failed during execution.", e);
        }
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
