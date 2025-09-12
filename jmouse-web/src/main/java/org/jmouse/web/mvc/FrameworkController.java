package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.context.BeanConditionIfProperty;
import org.jmouse.web.annotation.*;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.http.request.QueryParameters;
import org.jmouse.web.http.request.RequestAttributesHolder;
import org.jmouse.web.http.request.RequestPath;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * ⚙️ Built-in framework controller for jMouse MVC.
 *
 * <p>Provides default exception handling and error view rendering.
 * Can be enabled/disabled via the property:
 * <pre>{@code jmouse.mvc.frameworkController.enabled=true}</pre>
 * </p>
 */
@Controller
@BeanConditionIfProperty(name = "jmouse.mvc.frameworkController.enabled", value = "true")
public class FrameworkController {

    /**
     * ❌ Handle {@link HandlerMappingException} by preparing error details
     * for rendering in the default <code>jmouse/error</code> view.
     *
     * <p>Populates the model with request metadata, exception message,
     * stack traces, and a timestamp.</p>
     *
     * @param userAgent client {@code User-Agent} header
     * @param method    HTTP method of the request
     * @param response  servlet response (used to get current status code)
     * @param exception the thrown exception
     * @param model     MVC model to populate with error details
     * @return logical view name for error rendering
     */
    @ExceptionHandler({
            HandlerMappingException.class,
            HandlerAdapterException.class
    })
    public String exceptionHandler(
            @RequestHeader(HttpHeader.USER_AGENT) String userAgent,
            @StatusCode HttpStatus status,
            @RequestMethod HttpMethod method,
            HttpServletResponse response,
            Exception exception,
            Model model
    ) {
        response.setStatus(HttpStatus.NOT_FOUND.getCode());

        RequestPath     requestPath     = RequestAttributesHolder.getRequestPath();
        QueryParameters queryParameters = RequestAttributesHolder.getQueryParameters();
        HttpStatus httpStatus = HttpStatus.NOT_FOUND;

        model.addAttribute("userAgent", userAgent);
        model.addAttribute("httpStatus", httpStatus);
        model.addAttribute("statusCode", httpStatus.getCode());
        model.addAttribute("requestMethod", method);
        model.addAttribute("requestPath", requestPath.path());
        model.addAttribute("queryString", queryParameters.toString());
        model.addAttribute("message", getExceptionMessage(exception));
        model.addAttribute("stackTrace", getDetailedStackTrace(exception));
        model.addAttribute("timestamp", Instant.now().toString());

        return "view:jmouse/error";
    }

    /**
     * 📑 Collect stack traces for the given throwable and its causes.
     *
     * @param throwable top-level exception
     * @return map of exception message → stack trace
     */
    private Map<String, String> getDetailedStackTrace(Throwable throwable) {
        Map<String, String> stack     = new HashMap<>();
        Throwable           exception = throwable.getCause();

        stack.put(getExceptionMessage(throwable), toStringStackTrace(throwable));
        while (exception != null) {
            stack.put(getExceptionMessage(exception), toStringStackTrace(exception));
            exception = exception.getCause();
        }

        return stack;
    }

    /**
     * 📝 Convert stack trace to string for logging or rendering.
     *
     * @param throwable exception to format
     * @return string representation of the stack trace
     */
    private String toStringStackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    /**
     * 🏷️ Format exception type and message.
     *
     * @param throwable exception
     * @return formatted message in the form {@code ClassName :[message]}
     */
    private String getExceptionMessage(Throwable throwable) {
        return "%s :[%s]".formatted(throwable.getClass().getSimpleName(), throwable.getMessage());
    }
}
