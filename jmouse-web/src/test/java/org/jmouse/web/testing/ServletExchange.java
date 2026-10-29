package org.jmouse.web.testing;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.http.HttpMethod;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.util.*;

/** Minimal stateful servlet exchange for boundary tests, independent from an application container. */
public final class ServletExchange {
    public String method = "GET";
    public final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    public final Map<String, String[]> parameters = new HashMap<>();
    public final Map<String, Object> attributes = new HashMap<>();
    public final Map<String, String> responseHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    public int status = 200;
    public int sessionCreations;
    public int rotations;
    private HttpSession session;
    private String sessionId = UUID.randomUUID().toString();
    private final Map<String, Object> sessionAttributes = new HashMap<>();
    public final HttpServletRequest request;
    public final HttpServletResponse response;

    public ServletExchange() {
        request = proxy(HttpServletRequest.class, (proxy, call, arguments) -> {
            return switch (call.getName()) {
                case "getMethod" -> method;
                case "getRequestURI", "getServletPath" -> "/";
                case "getContextPath" -> "";
                case "getSession" -> session(arguments == null || arguments.length == 0 || Boolean.TRUE.equals(arguments[0]));
                case "changeSessionId" -> {
                    if (session == null) {
                        throw new IllegalStateException("No session");
                    }
                    rotations++;
                    sessionId = UUID.randomUUID().toString();
                    yield sessionId;
                }
                case "getHeader" -> headers.getOrDefault(arguments[0], List.of()).stream().findFirst().orElse(null);
                case "getHeaders" -> Collections.enumeration(headers.getOrDefault(arguments[0], List.of()));
                case "getHeaderNames" -> Collections.enumeration(headers.keySet());
                case "getParameter" -> {
                    String[] values = parameters.get(arguments[0]);
                    yield values == null || values.length == 0 ? null : values[0];
                }
                case "getParameterValues" -> parameters.get(arguments[0]);
                case "getParameterMap" -> parameters;
                case "getAttribute" -> attributes.get(arguments[0]);
                case "setAttribute" -> {
                    attributes.put((String) arguments[0], arguments[1]);
                    yield null;
                }
                case "removeAttribute" -> attributes.remove(arguments[0]);
                default -> defaultValue(call.getReturnType());
            };
        });
        response = proxy(HttpServletResponse.class, (proxy, call, arguments) -> {
            return switch (call.getName()) {
                case "sendError", "setStatus" -> {
                    status = (int) arguments[0];
                    yield null;
                }
                case "getStatus" -> status;
                case "setHeader", "addHeader" -> {
                    responseHeaders.put((String) arguments[0], (String) arguments[1]);
                    yield null;
                }
                case "getHeader" -> responseHeaders.get(arguments[0]);
                case "getHeaders" -> List.of(responseHeaders.get(arguments[0]));
                case "getHeaderNames" -> responseHeaders.keySet();
                default -> defaultValue(call.getReturnType());
            };
        });
        attributes.put(RequestRoute.REQUEST_ROUTE_ATTRIBUTE,
                new RequestRoute(HttpMethod.GET, null, null, null, null, Set.of(), request));
    }

    public RequestContext context() {
        return new RequestContext(request, response);
    }

    private HttpSession session(boolean create) {
        if (session == null && create) {
            sessionCreations++;
            session = proxy(HttpSession.class, (proxy, call, arguments) -> {
                return switch (call.getName()) {
                    case "getId" -> sessionId;
                    case "getAttribute" -> sessionAttributes.get(arguments[0]);
                    case "setAttribute" -> {
                        sessionAttributes.put((String) arguments[0], arguments[1]);
                        yield null;
                    }
                    case "removeAttribute" -> sessionAttributes.remove(arguments[0]);
                    case "getAttributeNames" -> Collections.enumeration(sessionAttributes.keySet());
                    case "invalidate" -> {
                        sessionAttributes.clear();
                        session = null;
                        yield null;
                    }
                    default -> defaultValue(call.getReturnType());
                };
            });
        }
        return session;
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        return null;
    }
}
