package org.jmouse.security.web.match;

import org.jmouse.web.match.routing.MappingMatcher;

public record RequestMatcherEntry<E>(MappingMatcher matcher, E entry) {}
