package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.http.request.ConditionalRequest;
import org.jmouse.web.http.request.ETag;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.request.PreconditionResult;
import org.jmouse.web.http.response.HeadersBuffer;
import org.jmouse.web.http.response.HttpServletHeadersBuffer;

final class PreconditionsGate {

    private PreconditionsGate() {}

    static boolean evaluateWithCommitIfNeeded(
            Headers requestHeaders, HttpServletResponse servletResponse, ETag etag, long lastModified) {
        HeadersBuffer      buffer = new HttpServletHeadersBuffer();
        PreconditionResult result = ConditionalRequest.evaluate(
                requestHeaders, buffer.getHeaders(), lastModified, etag);

        if (result != PreconditionResult.PROCEED_200) {
            buffer.write(servletResponse);
            return true;
        }

        return false;
    }
}
