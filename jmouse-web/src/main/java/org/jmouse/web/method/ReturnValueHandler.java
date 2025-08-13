package org.jmouse.web.method;

import org.jmouse.mvc.InvocationOutcome;
import org.jmouse.web.http.request.RequestContext;

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
     * @param outcome the actual outcome after method invocation
     * @return {@code true} if supported
     */
    boolean supportsReturnType(InvocationOutcome outcome);

    /**
     * 🧪 Handles the controller return value.
     *
     * <p>May write to the response, modify model attributes, or trigger view rendering.
     *
     * @param outcome the actual outcome
     * @param requestContext current request context
     */
    void handleReturnValue(InvocationOutcome outcome, RequestContext requestContext);
}
