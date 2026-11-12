package org.jmouse.web.mvc.result;

import org.jmouse.web.http.RequestContext;

/**
 * 🎯 Strategy for handling controller return values.
 *
 * <p>Used to transform or render the result of an invoked handler method.
 * Typically registered in the framework to support various return types
 * (e.g., {@code String} views, JSON, templates, etc).
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ReturnValueHandler {
    /**
     * 🔍 Checks if this handler can process the given return type.
     *
     * @return {@code true} if supported
     */
    boolean supportsReturnType(HandlerResult.ReturnValue result);

    /**
     * 🧪 Handles the controller return value.
     *
     * <p>May write to the response, modify model attributes, or trigger view rendering.
     */
    void handleReturnValue(HandlerResult.ReturnValue result, RequestContext requestContext);
}
