package org.jmouse.web.http;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;

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

    String WEB_REQUEST_HEADERS_ATTRIBUTE   = "WEB_REQUEST_HEADERS_ATTRIBUTE";
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

    default InetAddress getClientIp() {
        InetAddress clientIp = (InetAddress) getRequest().getAttribute(WEB_REQUEST_CLIENT_IP_ATTRIBUTE);;

        if (clientIp == null) {
            Headers headers       = getHeaders();
            String  xForwardedFor = (String) headers.getHeader(HttpHeader.X_FORWARDED_FOR);

            if (xForwardedFor != null) {
                String ip    = xForwardedFor;
                int    comma = xForwardedFor.indexOf(',');

                if (comma != -1) {
                    ip = xForwardedFor.substring(0, comma);
                }

                try {
                    clientIp = InetAddress.getByName(ip.trim());
                } catch (UnknownHostException ignored) {}
            }

            if (clientIp == null) {
                String xRealIp       = (String) getHeaders().getHeader(HttpHeader.X_REAL_IP);
                String remoteAddress = getRequest().getRemoteAddr();

                if (xRealIp != null) {
                    remoteAddress = xRealIp;
                }

                try {
                    clientIp = InetAddress.getByName(remoteAddress.trim());
                } catch (UnknownHostException ignored) {}
            }

            getRequest().setAttribute(WEB_REQUEST_CLIENT_IP_ATTRIBUTE, clientIp);
        }

        return clientIp;
    }

}
