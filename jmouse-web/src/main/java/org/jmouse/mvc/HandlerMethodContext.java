package org.jmouse.mvc;

import org.jmouse.web.method.HandlerMethod;
import org.jmouse.web.request.RequestContext;

public record HandlerMethodContext(RequestContext requestContext, HandlerMethod handlerMethod) {
}
