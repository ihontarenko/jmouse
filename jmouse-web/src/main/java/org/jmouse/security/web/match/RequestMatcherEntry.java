package org.jmouse.security.web.match;

import org.jmouse.security.web.RequestMatcher;

public record RequestMatcherEntry<E>(RequestMatcher matcher, E entry) {}
