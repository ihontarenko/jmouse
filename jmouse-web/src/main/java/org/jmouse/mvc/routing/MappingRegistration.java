package org.jmouse.mvc.routing;

import org.jmouse.mvc.Route;

public record MappingRegistration(Route route, Object handler) { }
