package org.jmouse.security.web;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.matcher.Matcher;

import java.util.List;

public interface SecurityFilterChain extends Matcher<HttpServletRequest> {

    boolean matches(HttpServletRequest request);

    List<Filter> getFilters();

}
