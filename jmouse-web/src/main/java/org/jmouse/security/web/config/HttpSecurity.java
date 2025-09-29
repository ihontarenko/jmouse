package org.jmouse.security.web.config;

import jakarta.servlet.Filter;
import org.jmouse.core.Sorter;
import org.jmouse.security.authorization.Accepter;
import org.jmouse.security.web.DefaultSecurityFilterChain;
import org.jmouse.security.web.RequestMatcher;

import java.util.ArrayList;
import java.util.List;

public final class HttpSecurity
        extends AbstractConfiguredSecurityBuilder<DefaultSecurityFilterChain, HttpSecurity>
        implements HttpSecurityBuilder<HttpSecurity> {

    private final List<Filter>   filters = new ArrayList<>();
    private       RequestMatcher matcher = RequestMatcher.any();

    @Override
    public HttpSecurity addFilter(Filter filter) {
        this.filters.add(filter);
        return this;
    }

    @Override
    public List<Filter> getFilters() {
        return this.filters;
    }

    @Override
    public HttpSecurity securityMatcher(RequestMatcher matcher) {
        this.matcher = matcher;
        return this;
    }

    @Override
    public HttpSecurity authorizeHttpRequests(Accepter<AuthorizeHttpRequestsConfigurer<HttpSecurity>> accepter) {
        accepter.accept(with(new AuthorizeHttpRequestsConfigurer<>()));
        return this;
    }

    public <C extends SecurityConfigurer<HttpSecurity>> C with(C configurer) {
        return apply(configurer);
    }

    @Override
    protected DefaultSecurityFilterChain doBuild() throws Exception {
        initializeConfigurers();
        configureConfigurers();

        List<Filter> sorted = new ArrayList<>(filters);
        sorted.sort(Sorter.PRIORITY_COMPARATOR);

        return new DefaultSecurityFilterChain(this.matcher, sorted);
    }
}