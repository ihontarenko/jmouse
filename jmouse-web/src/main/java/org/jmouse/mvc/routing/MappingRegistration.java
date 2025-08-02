package org.jmouse.mvc.routing;

public record MappingRegistration<T, M>(M mapping, T handler) { }
