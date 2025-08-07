package org.jmouse.mvc.adapter.return_value;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.mvc.InvocationOutcome;
import org.jmouse.mvc.RequestContext;
import org.jmouse.mvc.adapter.AbstractReturnValueHandler;
import org.jmouse.util.Priority;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.Headers;
import org.jmouse.web.request.http.HttpHeader;
import org.jmouse.web.request.http.HttpStatus;

/**
 * 🟦 Handles `null` return values from controller methods.
 * Sets a placeholder header to indicate no output is written.
 *
 * <p>💡 Typical usage: controller methods that return {@code void} or {@code null} explicitly.</p>
 *
 * <pre>{@code
 * @Get("/ping")
 * public void ping() {
 *     // No output written, handled by VoidReturnValueHandler
 * }
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @since 1.0
 */
@Priority(-1)
public class VoidReturnValueHandler extends AbstractReturnValueHandler {

    /**
     * ✅ Supports only {@code null} return values.
     *
     * @param outcome the MvcContainer with method return info
     * @return {@code true} if return value is {@code null}
     */
    @Override
    public boolean supportsReturnType(InvocationOutcome outcome) {
        return outcome.getReturnValue() == null || outcome.getMethodParameter().getParameterType() == void.class;
    }

    @Override
    protected void doReturnValueHandle(InvocationOutcome outcome, RequestContext requestContext) {
        HttpServletResponse response    = requestContext.response();
        String              contentType = response.getContentType();
        MediaType           consumes    = outcome.getHeaders().getContentType();

        if ((contentType == null || contentType.isBlank()) && consumes != null) {
            response.setContentType(consumes.toString());
        }

        HttpStatus httpStatus = outcome.getHttpStatus();

        response.setStatus(httpStatus == null ? HttpStatus.OK.getCode() : httpStatus.getCode());
        response.setHeader(HttpHeader.X_TEXT.value(), "NO-OP!");
    }

    /**
     * ⚙️ No initialization logic required.
     *
     * @param context current web context
     */
    @Override
    protected void doInitialize(WebBeanContext context) {
        // No-op
    }
}
