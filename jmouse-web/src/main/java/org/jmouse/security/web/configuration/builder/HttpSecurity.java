package org.jmouse.security.web.configuration.builder;

import jakarta.servlet.Filter;
import org.jmouse.core.Sorter;
import org.jmouse.security.web.*;
import org.jmouse.security.web.configuration.*;
import org.jmouse.security.web.configuration.configurer.*;

import java.util.ArrayList;
import java.util.List;

public final class HttpSecurity
        extends AbstractConfiguredSecurityBuilder<SecurityFilterChain, HttpSecurity>
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
    public HttpSecurity chainMatcher(RequestMatcher matcher) {
        this.matcher = matcher;
        return this;
    }

    public HttpSecurity securityContext(Customizer<SecurityContextConfigurer<HttpSecurity>> customizer) {
        customizer.customize(attach(new SecurityContextConfigurer<>()));
        return this;
    }

    public HttpSecurity sessionManagement(Customizer<SessionManagementConfigurer<HttpSecurity>> customizer) {
        customizer.customize(attach(new SessionManagementConfigurer<>()));
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

    public HttpSecurity submitForm(Customizer<SubmitFormConfigurer<HttpSecurity>> customizer) {
        customizer.customize(attach(new SubmitFormConfigurer<>()));
        return this;
    }

    public HttpSecurity httpBasic(Customizer<HttpBasicConfigurer<HttpSecurity>> customizer) {
        customizer.customize(attach(new HttpBasicConfigurer<>()));
        return this;
    }

    public HttpSecurity jwt(Customizer<JwtTokenConfigurer<HttpSecurity>> customizer) {
        customizer.customize(attach(new JwtTokenConfigurer<>()));
        return this;
    }

    public <C extends ConfigurerAdapter<SecurityFilterChain, HttpSecurity>> C attach(C configurer) {
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