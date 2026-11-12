package org.jmouse.web.servlet;

import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.BeanScope;
import org.jmouse.web.http.*;

/** Synchronous servlet request context. Partial initialization never leaves thread-bound metadata behind. */
public class RequestContextListener implements ServletRequestListener {

    @Override
    public void requestInitialized(ServletRequestEvent event) {
        if (!(event.getServletRequest() instanceof HttpServletRequest request)) {
            return;
        }
        RequestAttributesHolder.clear();
        try {
            RequestAttributesHolder.setRequestAttributes(
                RequestAttributes.ofRequest(BeanScope.REQUEST, request)
            );
            RequestPath path = RequestPath.ofRequest(request);
            request.setAttribute(RequestPath.REQUEST_PATH_ATTRIBUTE, path);
            RequestAttributesHolder.setRequestPath(path);
            RequestRoute route = RequestRoute.ofRequest(request);
            RequestAttributesHolder.setRequestRoute(route);
            request.setAttribute(QueryParameters.QUERY_PARAMETERS_ATTRIBUTE, route.queryParameters());
            RequestAttributesHolder.setQueryParameters(route.queryParameters());
            RequestHeaders headers = RequestHeaders.ofRequest(request);
            request.setAttribute(RequestHeaders.REQUEST_HEADERS_ATTRIBUTE, headers);
            RequestAttributesHolder.setRequestHeaders(headers);
            RequestParameters parameters = RequestParameters.ofRequest(request);
            request.setAttribute(RequestParameters.REQUEST_PARAMETERS_ATTRIBUTE, parameters);
            RequestAttributesHolder.setRequestParameters(parameters);
        } catch (RuntimeException | Error failure) {
            RequestAttributesHolder.clear();
            throw failure;
        }
    }

    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        RequestAttributesHolder.clear();
    }
}
