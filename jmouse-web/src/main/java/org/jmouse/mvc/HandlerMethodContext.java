package org.jmouse.mvc;

public record HandlerMethodContext(RequestContext requestContext, HandlerMethod handlerMethod) {
}
