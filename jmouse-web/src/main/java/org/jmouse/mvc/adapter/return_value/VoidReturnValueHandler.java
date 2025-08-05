package org.jmouse.mvc.adapter.return_value;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.mvc.InvocationOutcome;
import org.jmouse.mvc.MethodParameter;
import org.jmouse.mvc.RequestContext;
import org.jmouse.mvc.adapter.AbstractReturnValueHandler;
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
public class VoidReturnValueHandler extends AbstractReturnValueHandler {

    /**
     * ✅ Supports only {@code null} return values.
     *
     * @param result the MvcContainer with method return info
     * @return {@code true} if return value is {@code null}
     */
    @Override
    public boolean supportsReturnType(MethodParameter returnType, InvocationOutcome result) {
        return result.getReturnValue() == null;
    }

    @Override
    protected void doReturnValueHandle(InvocationOutcome result, RequestContext requestContext) {
        HttpServletResponse response    = requestContext.response();
        String              contentType = response.getContentType();
        Headers             headers     = result.getHeaders();
        MediaType           consumes    = MediaType.TEXT;

        if (contentType == null || contentType.isBlank()) {
            headers.setContentType(consumes);
        }

        if (response.getStatus() != 0) {
            result.setHttpStatus(HttpStatus.ofCode(response.getStatus()));
        }

        headers.setHeader(HttpHeader.X_TEXT, "NO-OP!");
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
