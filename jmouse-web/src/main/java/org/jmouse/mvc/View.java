package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * 🖼️ Interface representing a view that can render output to a HTTP response.
 *
 * <p>Implemented by rendering engines (e.g. template engines, JSP renderers, or static views)
 * to generate the final response based on the model data.
 *
 * <p>Used internally by {@code Dispatcher} and {@code ReturnValueHandler}.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @since 1.0
 */
public interface View {

    /**
     * 🚀 Renders the view using the provided model and HTTP request/response.
     *
     * @param model    the model data to expose to the view
     * @param request  the current HTTP servlet request
     * @param response the HTTP servlet response to write the output to
     */
    void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response);
}
