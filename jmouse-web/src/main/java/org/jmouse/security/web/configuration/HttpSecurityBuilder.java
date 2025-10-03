package org.jmouse.security.web.configuration;

import jakarta.servlet.Filter;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.SecurityFilterChain;

import java.util.List;
import java.util.function.Supplier;

public interface HttpSecurityBuilder<H extends HttpSecurityBuilder<H>>
        extends SecurityBuilder<SecurityFilterChain> {

    default <U> U getObject(Class<U> type) {
        return getObject(type, () -> null);
    }

    H addFilter(Filter filter);

    List<Filter> getFilters();

    <U> void setSharedObject(Class<U> type, U object);

    <U> U getSharedObject(Class<U> type);

    <U> U getObject(Class<U> type, Supplier<U> defaultObject);

    <C extends SecurityConfigurer<SecurityFilterChain, H>> void removeConfigurer(Class<C> type);

    <C extends SecurityConfigurer<SecurityFilterChain, H>> C getConfigurer(Class<C> type);

    H chainMatcher(RequestMatcher matcher);

}
