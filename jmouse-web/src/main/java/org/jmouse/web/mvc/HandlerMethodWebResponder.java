package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.request.ETag;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.response.HttpServletHeadersBuffer;

import static org.jmouse.web.http.request.ConditionalRequest.evaluate;
import static org.jmouse.web.http.request.PreconditionResult.PROCEED_200;

public class HandlerMethodWebResponder extends WebResponder {

    private static final ThreadLocal<Headers> THREAD_LOCAL = new ThreadLocal<>();

    public HandlerMethodWebResponder() {
        super(true);
    }

    public boolean applyPreconditions(Headers requestHeaders, ETag etag, long lastModified) {
        return evaluate(requestHeaders, getHeaders(), lastModified, etag) != PROCEED_200;
    }

    protected final void beforeResponse() {
        THREAD_LOCAL.set(new Headers());
    }

    protected final void afterResponse() {
        try {
            cleanupHeaders();
        } finally {
            THREAD_LOCAL.remove();
        }
    }

    @Override
    public void writeHeaders(HttpServletResponse response) {
        new HttpServletHeadersBuffer(getHeaders()).write(response);
    }

    @Override
    public Headers getHeaders() {
        Headers headers = THREAD_LOCAL.get();

        if (headers == null) {
            headers = new Headers();
            THREAD_LOCAL.set(headers);
        }

        return headers;
    }

    @Override
    public void cleanupHeaders() {
        Headers headers = THREAD_LOCAL.get();
        if (headers != null) {
            headers.clear();
        }
    }

    @Override
    public boolean cleanupHeaders(HttpServletResponse response) {
        cleanupHeaders();
        return new HttpServletHeadersBuffer(getHeaders()).cleanup(response);
    }
}
