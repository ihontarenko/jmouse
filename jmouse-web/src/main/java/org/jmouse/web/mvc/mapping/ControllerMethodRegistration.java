package org.jmouse.web.mvc.mapping;

import org.jmouse.web.mvc.Route;
import org.jmouse.web.mvc.adapter.ControllerMethod;

public record ControllerMethodRegistration(Route route, ControllerMethod controllerMethod) { }
