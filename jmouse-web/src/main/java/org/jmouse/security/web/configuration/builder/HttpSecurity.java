package org.jmouse.security.web.configuration.builder;

import jakarta.servlet.Filter;
import org.jmouse.core.Customizer;
import org.jmouse.core.Sorter;
import org.jmouse.security.UserPrincipalService;
import org.jmouse.security.web.*;
import org.jmouse.security.web.configuration.*;
import org.jmouse.security.web.configuration.configurer.*;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.MappingMatcher;
import org.jmouse.web.match.routing.MatcherCriteria;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class HttpSecurity
        extends AbstractConfiguredSecurityBuilder<MatchableSecurityFilterChain, HttpSecurity>
        implements HttpSecurityBuilder<HttpSecurity> {

    private final List<Filter>                 filters = new ArrayList<>();
    private       MappingMatcher<RequestRoute> matcher = MatcherCriteria.any();

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
    public HttpSecurity addFilterAfter(Filter filter, Class<? extends Filter> afterFilter) {
        return addFilterOfOffset(filter, 1, afterFilter);
    }

    @Override
    public HttpSecurity addFilterBefore(Filter filter, Class<? extends Filter> beforeFilter) {
        return addFilterOfOffset(filter, -1, beforeFilter);
    }

    private HttpSecurity addFilterOfOffset(Filter filter, int offset, Class<? extends Filter> registered) {
        SecurityFilterOrder orders          = getSharedObject(SharedAttributes.SECURITY_FILTER_ORDER);
        Filter              unwrappedFilter = filter;

        if (filter instanceof OrderedFilter orderedFilter) {
            unwrappedFilter = orderedFilter.filter();
        }

        int order = orders.getOrder(registered);

        addFilter(new OrderedFilter(unwrappedFilter, order + offset));

        return this;
    }

    @Override
    public List<Filter> getFilters() {
        List<Filter> filters = new ArrayList<>(Set.copyOf(this.filters));
        filters.sort(Sorter.PRIORITY_COMPARATOR);
        return filters;
    }

    @Override
    public HttpSecurity chainMatcher(Customizer<MatcherCriteria> customizer) {
        MatcherCriteria matcher = new MatcherCriteria();
        customizer.customize(matcher);
        this.matcher = matcher;
        return this;
    }

    public HttpSecurity principalService(UserPrincipalService principalService) {
        setSharedObject(UserPrincipalService.class, principalService);
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

    public HttpSecurity authorization(
            Customizer<AuthorizeRequestConfigurer<HttpSecurity>.AuthorizationRequestMatchCriterion> customizer) {
        customizer.customize(attach(new AuthorizeRequestConfigurer<>()).getRequestMatchCriterion());
        return this;
    }

    public HttpSecurity authentication(
            Customizer<AuthenticationConfigurer<HttpSecurity>> customizer) {
        customizer.customize(new AuthenticationConfigurer<>(this::attach));
        return this;
    }

    public HttpSecurity exceptionHandling(Customizer<ExceptionHandlingConfigurer<HttpSecurity>> c) {
        c.customize(attach(new ExceptionHandlingConfigurer<>()));
        return this;
    }

    public <C extends ConfigurerAdapter<MatchableSecurityFilterChain, HttpSecurity>> C attach(C configurer) {
        return apply(configurer);
    }

    @Override
    protected SecurityFilterChain doBuild() throws Exception {
        initializeConfigurers();
        configureConfigurers();

        List<Filter> unique = new ArrayList<>(Set.copyOf(this.filters));
        unique.sort(Sorter.PRIORITY_COMPARATOR);

        return new SecurityFilterChain(this.matcher, unique);
    }
}