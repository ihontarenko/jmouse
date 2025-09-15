package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.context.BeanConditionIfProperty;
import org.jmouse.web.annotation.*;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.request.QueryParameters;
import org.jmouse.web.http.request.RequestAttributesHolder;
import org.jmouse.web.http.request.RequestPath;
import org.jmouse.web.mvc.resource.ResourceNotFoundException;
import org.jmouse.web.mvc.resource.ResourceValidationFailedException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 🧰 Built-in MVC fallback controller for framework-level errors.
 *
 * <p>Maps common framework exceptions to a branded error view and populates
 * a minimal diagnostics model (method, path, query, status, message, stack traces).</p>
 *
 * <p>Enabled when property {@code jmouse.mvc.frameworkController.enabled=true}.</p>
 *
 * @see Controller
 * @see ExceptionHandler
 * @see HttpStatus
 */
@Controller
@BeanConditionIfProperty(name = "jmouse.mvc.frameworkController.enabled", value = "true")
public class FrameworkController {

    /**
     * Handles resolution/dispatching errors and renders {@code 404 Not Found}.
     *
     * @param response  target servlet response (status will be set)
     * @param exception the thrown exception
     * @param model     view model to populate
     * @return view name of the framework error page
     * @see HandlerMappingException
     * @see HandlerAdapterException
     */
    @ExceptionHandler({
            HandlerMappingException.class,
            NotFoundException.class,
            HandlerAdapterException.class,
            ResourceNotFoundException.class,
    })
    public String handle404(HttpServletResponse response, Exception exception, Model model) {
        return getGeneratedResponse(model, HttpStatus.NOT_FOUND, exception, response);
    }

    /**
     * Handles resource validation failures and renders {@code 422 Unprocessable Entity}.
     *
     * @param response  target servlet response (status will be set)
     * @param exception validation exception to report
     * @param model     view model to populate with diagnostics
     * @see ResourceValidationFailedException
     * @see HttpStatus#UNPROCESSABLE_ENTITY
     */
    @ExceptionHandler({
            ResourceValidationFailedException.class,
    })
    public void handle422(HttpServletResponse response, Exception exception, Model model) {
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.getCode());
        response.setHeader(HttpHeader.X_JMOUSE_DEBUG.value(), exception.getMessage());
    }

    /**
     * Handles unexpected runtime failures and renders {@code 500 Internal Server Error}.
     *
     * @param response  target servlet response (status will be set)
     * @param exception the thrown exception
     * @param model     view model to populate
     * @return view name of the framework error page
     */
    @ExceptionHandler({
            IllegalStateException.class
    })
    public String handle500(HttpServletResponse response, Exception exception, Model model) {
        return getGeneratedResponse(model, HttpStatus.INTERNAL_SERVER_ERROR, exception, response);
    }

    /**
     * Builds a standard error model, sets HTTP status, and returns the error view.
     *
     * <p>Model attributes:</p>
     * <ul>
     *   <li><b>userAgent</b> — request User-Agent string</li>
     *   <li><b>httpStatus</b>, <b>statusCode</b> — status enum and numeric code</li>
     *   <li><b>requestMethod</b>, <b>requestPath</b>, <b>queryString</b> — request summary</li>
     *   <li><b>message</b> — concise exception message</li>
     *   <li><b>stackTrace</b> — map of exception messages to stack traces (root → causes)</li>
     *   <li><b>timestamp</b> — ISO-8601 instant</li>
     * </ul>
     *
     * @param model      view model to populate
     * @param httpStatus status to set on the response and expose in the model
     * @param exception  source exception
     * @param response   target servlet response
     * @return {@code "view:jmouse/error"} view name
     */
    private String getGeneratedResponse(Model model, HttpStatus httpStatus, Exception exception, HttpServletResponse response) {
        Headers         headers         = RequestAttributesHolder.getRequestHeaders().headers();
        QueryParameters queryParameters = RequestAttributesHolder.getQueryParameters();
        RequestPath     requestPath     = RequestAttributesHolder.getRequestPath();

        model.addAttribute("userAgent", headers.getUserAgent());
        model.addAttribute("httpStatus", httpStatus);
        model.addAttribute("statusCode", httpStatus.getCode());
        model.addAttribute("requestMethod", headers.getMethod());
        model.addAttribute("requestPath", requestPath.path());
        model.addAttribute("queryString", queryParameters.toString());
        model.addAttribute("message", getExceptionMessage(exception));
        model.addAttribute("stackTrace", getDetailedStackTrace(exception));
        model.addAttribute("timestamp", Instant.now().toString());

        response.setStatus(httpStatus.getCode());

        return "view:jmouse/error";
    }

    /**
     * Collects the full stack trace for the throwable and each of its causes.
     *
     * <p>Map keys are concise messages (class + message); values are full stack traces.</p>
     *
     * @param throwable root throwable
     * @return ordered map of messages to stack-trace strings (root followed by causes)
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
     * Converts a throwable's stack trace to a string.
     *
     * @param throwable source throwable
     * @return full stack trace as a string
     */
    private String toStringStackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    /**
     * Builds a concise message in the form {@code SimpleClassName :[message]}.
     *
     * @param throwable source throwable
     * @return concise message string
     */
    private String getExceptionMessage(Throwable throwable) {
        return "%s :[%s]".formatted(throwable.getClass().getSimpleName(), throwable.getMessage());
    }
}
