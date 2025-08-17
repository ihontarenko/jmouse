package org.jmouse.web.mvc.method;

import org.jmouse.web.http.request.RequestContext;

public record HandlerMethodContext(RequestContext requestContext, HandlerMethod handlerMethod) {
}
