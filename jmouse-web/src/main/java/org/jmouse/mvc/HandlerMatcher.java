package org.jmouse.mvc;

import jakarta.servlet.http.HttpServletRequest;

public interface HandlerMatcher<H> {
    MatchedRoute<H> match(HttpServletRequest request);
}
