package org.jmouse.security.web.configuration;

import jakarta.servlet.Filter;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.SecurityFilterChain;

import java.util.List;

public interface HttpSecurityBuilder<H extends HttpSecurityBuilder<H>>
        extends SecurityBuilder<SecurityFilterChain> {

    H addFilter(Filter filter);

    List<Filter> getFilters();

    <U> void setSharedObject(Class<U> type, U object);

    <U> U getSharedObject(Class<U> type);

    <C extends SecurityConfigurer<SecurityFilterChain, H>> void removeConfigurer(Class<C> type);

    <C extends SecurityConfigurer<SecurityFilterChain, H>> C getConfigurer(Class<C> type);

    H chainMatcher(RequestMatcher matcher);

}
