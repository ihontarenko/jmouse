package org.jmouse.web.mvc.cors;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.http.request.Allow;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.request.Vary;

import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNullElse;
import static org.jmouse.web.http.HttpHeader.*;

public class WebCorsProcessor implements CorsProcessor {

    public static final String MATCH_ALL               = "*";
    public static final String INVALID_CORS_REQUEST    = "Invalid CORS request!";
    public static final String METHOD_IS_NOT_ALLOWED   = "CORS: requested method is not allowed";
    public static final String HEADERS_ARE_NOT_ALLOWED = "CORS: requested headers are not allowed";
    public static final String ORIGIN_NOT_ALLOWED      = "CORS: Origin not allowed!";

    private static boolean isOriginAllowed(String origin, Set<String> allowed) {
        return allowed.contains("*") || allowed.contains(origin);
    }

    private static boolean reject(Headers responseHeaders, String message) {
        responseHeaders.setStatus(HttpStatus.FORBIDDEN);
        responseHeaders.setHeader(HttpHeader.X_JMOUSE_DEBUG, requireNonNullElse(message, INVALID_CORS_REQUEST));
        return false;
    }

    @Override
    public boolean handleRequest(
            CorsConfiguration configuration, Headers requestHeaders, Headers responseHeaders, boolean preflight) {
        responseHeaders.setHeader(VARY, Vary.of(ORIGIN, ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_REQUEST_HEADERS)
                .toHeaderValue());

        if (!Cors.isCorsRequest(requestHeaders)) {
            return true;
        }

        if (responseHeaders.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN) != null) {
            return true;
        }

        return doHandleRequest(configuration, requestHeaders, responseHeaders, preflight);
    }

    protected boolean doHandleRequest(
            CorsConfiguration configuration, Headers requestHeaders, Headers responseHeaders, boolean preflight) {
        Matcher<String> originMatcher = new OriginMatcher(List.copyOf(configuration.getAllowedOrigins()));
        String          origin        = (String) requestHeaders.getHeader(ORIGIN);

        if (!checkOrigin(origin, originMatcher)) {
            return reject(responseHeaders, ORIGIN_NOT_ALLOWED);
        }

        HttpMethod httpMethod = getHttpMethod(requestHeaders, preflight);
        if (!checkHttpMethod(httpMethod, configuration)) {
            return reject(responseHeaders, "CORS: method '" + httpMethod + "' is not allowed");
        }

        List<HttpHeader> accessHeaders = getRequestHeaders(requestHeaders, preflight);
        if (!checkRequestHeaders(accessHeaders, configuration)) {
            return reject(responseHeaders, "CORS: headers '" + accessHeaders + "' are not allowed");
        }

        responseHeaders.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);

        if (preflight) {
            responseHeaders.setHeader(ACCESS_CONTROL_ALLOW_METHODS, configuration.getAllowedMethods());
        }

        if (preflight) {

        }

        return true;
    }

    public boolean checkOrigin(String origin, Matcher<String> originMatcher) {
        return originMatcher.matches(origin);
    }

    public boolean checkHttpMethod(HttpMethod method, CorsConfiguration configuration) {
        if (method == null) {
            return false;
        }

        Set<HttpMethod> allowed = configuration.getAllowedMethods();

        return Allow.of(allowed).contains(method);
    }

    public boolean checkRequestHeaders(List<HttpHeader> requestHeaders, CorsConfiguration configuration) {
        if (requestHeaders == null || requestHeaders.isEmpty()) {
            return true;
        }

        Set<HttpHeader> allowed = configuration.getAllowedHeaders();

        if (allowed.isEmpty()) {
            return true;
        }

        for (HttpHeader httpHeader : requestHeaders) {
            if (!allowed.contains(httpHeader)) {
                return false;
            }
        }

        return true;
    }

    public boolean validateAllowCredentials(CorsConfiguration configuration) {
        return !configuration.isAllowCredentials() || configuration.getAllowedOrigins() == null || !configuration.getAllowedOrigins()
                .contains(MATCH_ALL);
    }

    public boolean validateAllowPrivateNetwork(CorsConfiguration configuration) {
        return !configuration.isAllowPrivateNetwork() || configuration.getAllowedOrigins() == null || !configuration.getAllowedOrigins()
                .contains(MATCH_ALL);
    }

    private HttpMethod getHttpMethod(Headers requestHeaders, boolean preflight) {
        HttpMethod httpMethod = requestHeaders.getMethod();

        if (preflight) {
            httpMethod = HttpMethod.ofName(
                    (String) requestHeaders.getHeader(ACCESS_CONTROL_REQUEST_METHOD)
            );
        }

        return httpMethod;
    }

    private List<HttpHeader> getRequestHeaders(Headers requestHeaders, boolean preflight) {
        List<HttpHeader> headers = List.copyOf(requestHeaders.asMap().keySet());

        if (preflight) {
            headers = Vary.of((String) requestHeaders.getHeader(ACCESS_CONTROL_REQUEST_HEADERS)).asList();
        }

        return headers;
    }

}
