package org.jmouse.mvc.mapping;

import org.jmouse.mvc.Route;
import org.jmouse.mvc.adapter.ControllerMethod;

public record ControllerMethodRegistration(Route route, ControllerMethod functionalRoute) { }
