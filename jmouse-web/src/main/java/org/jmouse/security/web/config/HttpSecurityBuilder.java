package org.jmouse.security.web.config;

import jakarta.servlet.Filter;
import org.jmouse.security.authorization.Accepter;
import org.jmouse.security.web.DefaultSecurityFilterChain;
import org.jmouse.security.web.RequestMatcher;

import java.util.List;

public interface HttpSecurityBuilder<H extends HttpSecurityBuilder<H>>
        extends SecurityBuilder<DefaultSecurityFilterChain> {

    H addFilter(Filter filter);

    List<Filter> getFilters();

    H securityMatcher(RequestMatcher matcher);

    H authorizeHttpRequests(Accepter<AuthorizeHttpRequestsConfigurer<H>> accepter);

}
