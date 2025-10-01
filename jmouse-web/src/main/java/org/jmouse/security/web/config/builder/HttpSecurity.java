package org.jmouse.security.web.config.builder;

import jakarta.servlet.Filter;
import org.jmouse.core.Sorter;
import org.jmouse.security.web.DefaultSecurityFilterChain;
import org.jmouse.security.web.OrderedFilter;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.SecurityFilterOrder;
import org.jmouse.security.web.config.*;
import org.jmouse.security.web.config.configurer.AnonymousConfigurer;
import org.jmouse.security.web.config.configurer.AuthorizeHttpRequestsConfigurer;
import org.jmouse.security.web.config.configurer.ExceptionHandlingConfigurer;

import java.util.ArrayList;
import java.util.List;

public final class HttpSecurity
        extends AbstractConfiguredSecurityBuilder<DefaultSecurityFilterChain, HttpSecurity>
        implements HttpSecurityBuilder<HttpSecurity> {

    private final List<Filter>   filters = new ArrayList<>();
    private       RequestMatcher matcher = RequestMatcher.any();

    public HttpSecurity() {
        setSharedObject(SecurityFilterOrder.class, new SecurityFilterOrder());
    }

    @Override
    public HttpSecurity addFilter(Filter filter) {
        Filter ordered = filter;

        if (!(ordered instanceof OrderedFilter)) {
            SecurityFilterOrder orders = getSharedObject(SharedAttributes.SECURITY_FILTER_ORDER);
            ordered = new OrderedFilter(ordered, orders.getOrder(filter.getClass()));
        }

        this.filters.add(ordered);

        return this;
    }

    @Override
    public List<Filter> getFilters() {
        return this.filters;
    }

    @Override
    public HttpSecurity securityChainMatcher(RequestMatcher matcher) {
        this.matcher = matcher;
        return this;
    }

    public HttpSecurity anonymous(Customizer<AnonymousConfigurer<HttpSecurity>> customizer) {
        customizer.customize(attach(new AnonymousConfigurer<>()));
        return this;
    }

    public HttpSecurity authorizeHttpRequests(Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.Registry> customizer) {
        customizer.customize(attach(new AuthorizeHttpRequestsConfigurer<>()).getRegistry());
        return this;
    }

    public HttpSecurity exceptionHandling(Customizer<ExceptionHandlingConfigurer<HttpSecurity>> c) {
        c.customize(attach(new ExceptionHandlingConfigurer<>()));
        return this;
    }

    public <C extends SecurityConfigurer<HttpSecurity>> C attach(C configurer) {
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