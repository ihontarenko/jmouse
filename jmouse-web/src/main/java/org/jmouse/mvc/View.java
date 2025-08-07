package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;

import java.util.Map;

/**
 * 🖼️ Interface representing a view that can render output to a HTTP response.
 *
 * <p>Implemented by rendering engines (e.g. view engines, JSP renderers, or static views)
 * to generate the final response based on the model data.</p>
 *
 * <p>Used internally by {@code org.jmouse.mvc.FrameworkDispatcher} and {@code org.jmouse.mvc.ReturnValueHandler}.</p>
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

    /**
     * 📦 Returns the content type this view produces (e.g. {@code text/html}, {@code application/json}).
     *
     * <p>This is used to set the {@code Content-Type} header in the response.</p>
     *
     * @return the {@link MediaType} this view outputs
     */
    MediaType getContentType();
}
