package org.jmouse.web.http;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.jmouse.http.Headers;
import org.jmouse.http.HttpMethod;

/**
 * 🌐 Abstraction over a web request.
 *
 * <p>Provides access to the underlying {@link HttpServletRequest}
 * and HTTP method of the current request.</p>
 *
 * <p>Extends {@link RequestAttributes} for attribute management.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 */
public interface WebRequest extends RequestAttributes {
    String WEB_REQUEST_HEADERS_ATTRIBUTE = "WEB_REQUEST_HEADERS_ATTRIBUTE";
    String WEB_REQUEST_CLIENT_IP_ATTRIBUTE = "WEB_REQUEST_CLIENT_IP_ATTRIBUTE";

    /**
     * 🔍 Get the native servlet request.
     *
     * @return the underlying {@link HttpServletRequest}
     */
    HttpServletRequest getRequest();

    /**
     * ⚡ Get the HTTP method (GET, POST, etc).
     *
     * @return the HTTP method as {@link HttpMethod}
     */
    HttpMethod getHttpMethod();

    default Headers getHeaders() {
        Headers headers = (Headers) getRequest().getAttribute(WEB_REQUEST_HEADERS_ATTRIBUTE);

        if (headers == null) {
            RequestHeaders requestHeaders = RequestAttributesHolder.getRequestHeaders();
            headers = requestHeaders.headers();
            getRequest().setAttribute(WEB_REQUEST_HEADERS_ATTRIBUTE, headers);
        }

        return headers;
    }

    /** The servlet container owns trusted proxy normalization. Raw forwarding headers are not trusted here. */
    default InetAddress getClientIp() {
        return resolveClientAddress(getRequest());
    }

    static InetAddress resolveClientAddress(HttpServletRequest request) {
        if (request.getAttribute(WEB_REQUEST_CLIENT_IP_ATTRIBUTE) instanceof InetAddress cached) {
            return cached;
        }
        try {
            InetAddress address = InetAddress.getByName(request.getRemoteAddr());
            request.setAttribute(WEB_REQUEST_CLIENT_IP_ATTRIBUTE, address);
            return address;
        } catch (UnknownHostException failure) {
            throw new IllegalStateException("Invalid remote address supplied by servlet container", failure);
        }
    }
}
