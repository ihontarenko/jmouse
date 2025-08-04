package org.jmouse.mvc.adapter.return_value;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.mvc.InvocationResult;
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
     * @param returnType the MvcContainer with method return info
     * @return {@code true} if return value is {@code null}
     */
    @Override
    public boolean supportsReturnType(InvocationResult returnType) {
        return returnType.getReturnValue() == null;
    }

    /**
     * 🧩 Writes nothing to response, but adds debug header {@code X-TEXT: NO-OP!}.
     *
     * @param mvcContainer controller invocation result
     * @param request      HTTP request
     * @param response     HTTP response
     */
    @Override
    protected void doReturnValueHandle(InvocationResult mvcContainer, HttpServletRequest request, HttpServletResponse response) {

        mvcContainer.getHeaders().setHeader(HttpHeader.X_TEXT, "NO-OP!");

        String    contentType = response.getContentType();
        Headers   headers     = mvcContainer.getHeaders();
        MediaType consumes    = MediaType.TEXT;

        if (contentType == null || contentType.isBlank()) {
            headers.setContentType(consumes);
        }

        if (response.getStatus() != 0) {
            mvcContainer.setHttpStatus(HttpStatus.ofCode(response.getStatus()));
        }

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
