package org.jmouse.security.web.configuration;

import jakarta.servlet.Filter;
import org.jmouse.core.Customizer;
import org.jmouse.security.web.MatchableSecurityFilterChain;
import org.jmouse.web.match.routing.MatcherCriteria;

import java.util.List;
import java.util.function.Supplier;

public interface HttpSecurityBuilder<H extends HttpSecurityBuilder<H>>
        extends SecurityBuilder<MatchableSecurityFilterChain> {

    default <U> U getObject(Class<U> type) {
        return getObject(type, () -> null);
    }

    H addFilter(Filter filter);

    H addFilterAfter(Filter filter, Class<? extends Filter> afterFilter);

    H addFilterBefore(Filter filter, Class<? extends Filter> beforeFilter);

    List<Filter> getFilters();

    <U> void setSharedObject(Class<U> type, U object);

    <U> U getSharedObject(Class<U> type);

    <U> U getObject(Class<U> type, Supplier<U> defaultObject);

    <C extends SecurityConfigurer<MatchableSecurityFilterChain, H>> void removeConfigurer(Class<C> type);

    <C extends SecurityConfigurer<MatchableSecurityFilterChain, H>> C getConfigurer(Class<C> type);

    H chainMatcher(Customizer<MatcherCriteria> customizer);

}
