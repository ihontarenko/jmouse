package org.jmouse.web.mvc.method;

import org.jmouse.web.http.RequestContext;

public record HandlerMethodContext(RequestContext requestContext, HandlerMethod handlerMethod) {
}
