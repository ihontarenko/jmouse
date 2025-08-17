package org.jmouse.web.mvc.adapter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.mvc.HandlerAdapter;
import org.jmouse.web.mvc.HandlerAdapterException;
import org.jmouse.web.mvc.MVCResult;
import org.jmouse.web.mvc.MappedHandler;

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
public class ControllerMethodHandlerAdapter implements HandlerAdapter {

    /**
     * 🛠 Handles the request by invoking {@link ControllerMethod}.
     *
     * <ul>
     *   <li>Ensures default content type if not set</li>
     *   <li>Flushes the response buffer</li>
     * </ul>
     *
     * @throws HandlerAdapterException in case of I/O failure
     */
    @Override
    public MVCResult handle(HttpServletRequest request, HttpServletResponse response,
                            MappedHandler mappedHandler) {
        try {
            ((ControllerMethod) mappedHandler.handler()).handle(request, response);
        } catch (IOException e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
            throw new HandlerAdapterException("Handler failed during execution.", e);
        }

        return null;
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
