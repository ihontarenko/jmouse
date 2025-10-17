package org.jmouse.security.web;

import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.MappingMatcher;

public record RequestMatcherEntry<E>(MappingMatcher<RequestRoute> matcher, E entry) {}
