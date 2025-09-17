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
            return reject(responseHeaders, METHOD_IS_NOT_ALLOWED);
        }

        List<HttpHeader> accessHeaders = getRequestHeaders(requestHeaders, preflight);
        if (preflight && !checkRequestHeaders(accessHeaders, configuration)) {
            return reject(responseHeaders, HEADERS_ARE_NOT_ALLOWED);
        }

        Set<String> allowedOrigins   = configuration.getAllowedOrigins();
        boolean     allowCredentials = configuration.isAllowCredentials();
        boolean     isAnyOrigin      = allowedOrigins != null && allowedOrigins.contains(MATCH_ALL);
        String      allowOriginValue = (allowCredentials && isAnyOrigin) ? origin : (isAnyOrigin ? "*" : origin);

        responseHeaders.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, allowOriginValue);

        if (allowCredentials) {
            responseHeaders.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE.toString());
        }

        if (preflight) {
            // Allow-Methods
            Allow allowMethods = getAllowedMethods(configuration.getAllowedMethods(), httpMethod);
            responseHeaders.setHeader(ACCESS_CONTROL_ALLOW_METHODS, allowMethods.toHeaderValue());

            // Allow-Headers
            Set<HttpHeader>  allowedHeaders   = configuration.getAllowedHeaders();
            List<HttpHeader> requestedHeaders = getRequestHeaders(requestHeaders, true);
            Vary             requestedVary    = Vary.of(requestedHeaders);

            if ((allowedHeaders == null || allowedHeaders.isEmpty()) && !requestedVary.isEmpty()) {
                responseHeaders.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, requestedVary.toHeaderValue());
            } else {
                Vary allowedVary = Vary.of(allowedHeaders);
                Vary grantedVary = allowedVary.intersect(requestedVary);
                if (!grantedVary.isEmpty()) {
                    responseHeaders.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, grantedVary.toHeaderValue());
                }
            }

            // Max-Age
            if (configuration.getMaxAge() > 0) {
                responseHeaders.setHeader(ACCESS_CONTROL_MAX_AGE, String.valueOf(configuration.getMaxAge()));
            }

            // Private-Network
            if (configuration.isAllowPrivateNetwork()) {
                boolean privateNetwork = Boolean.getBoolean(
                        (String) requestHeaders.getHeader(ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK));
                if (privateNetwork) {
                    responseHeaders.setHeader(ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK, "true");
                }
            }

            responseHeaders.setStatus(HttpStatus.NO_CONTENT);

            return false;
        }

        // Exposed-Headers
        Vary exposedVary = Vary.of(configuration.getExposedHeaders());
        if (!exposedVary.isEmpty()) {
            responseHeaders.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, exposedVary.toHeaderValue());
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

    private Allow getAllowedMethods(Set<HttpMethod> allowedMethods, HttpMethod fallback) {
        Allow allow = Allow.of(allowedMethods);

        if (allow.isEmpty()) {
            allow = allow.with(fallback);
        }

        return allow;
    }

}
