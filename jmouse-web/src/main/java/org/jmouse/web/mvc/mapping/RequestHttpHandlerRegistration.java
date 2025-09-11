package org.jmouse.web.mvc.mapping;

import org.jmouse.web.mvc.Route;
import org.jmouse.web.mvc.adapter.RequestHttpHandler;

public record RequestHttpHandlerRegistration(Route route, RequestHttpHandler handler) { }
