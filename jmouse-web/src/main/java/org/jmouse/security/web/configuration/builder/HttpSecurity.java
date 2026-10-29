package org.jmouse.security.web.configuration.builder;

import jakarta.servlet.Filter;
import org.jmouse.core.Customizer;
import org.jmouse.security.UserPrincipalService;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.authentication.AuthenticationResolverRegistry;
import org.jmouse.security.authentication.ResolversAuthenticationManager;
import org.jmouse.security.authentication.UsernamePasswordAuthentication;
import org.jmouse.security.authentication.dao.DaoAuthenticationResolver;
import org.jmouse.security.password.PasswordEncoder;
import org.jmouse.security.password.Pbkdf2Sha256PasswordEncoder;
import org.jmouse.security.web.*;
import org.jmouse.security.web.configuration.*;
import org.jmouse.security.web.configuration.configurer.*;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.MappingMatcher;
import org.jmouse.web.match.routing.MatcherCriteria;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.function.Supplier;

public final class HttpSecurity
        extends AbstractConfiguredSecurityBuilder<MatchableSecurityFilterChain, HttpSecurity>
        implements HttpSecurityBuilder<HttpSecurity> {

    private final static Supplier<SecurityFilterOrder> DEFAULT_ORDER_SUPPLIER = SecurityFilterOrder::new;

    private final List<Filter>                 filters = new ArrayList<>();
    private       MappingMatcher<RequestRoute> matcher = MatcherCriteria.any();

    public HttpSecurity() {
        attach(new CsrfConfigurer<>());
    }

    @Override
    public HttpSecurity addFilter(Filter filter) {
        Filter ordered = filter;

        if (!(ordered instanceof OrderedFilter)) {
            SecurityFilterOrder orders = getObject(SharedAttributes.SECURITY_FILTER_ORDER, DEFAULT_ORDER_SUPPLIER);
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
        SecurityFilterOrder orders          = getObject(SharedAttributes.SECURITY_FILTER_ORDER, DEFAULT_ORDER_SUPPLIER);
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
        List<Filter> filters = new ArrayList<>(new LinkedHashSet<>(this.filters));
        filters.sort(java.util.Comparator.comparingInt(filter -> ((OrderedFilter) filter).order()));
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

    public HttpSecurity authenticationManager(AuthenticationManager manager) {
        setSharedObject(AuthenticationManager.class, java.util.Objects.requireNonNull(manager, "manager"));
        return this;
    }

    public HttpSecurity passwordEncoder(PasswordEncoder encoder) {
        setSharedObject(PasswordEncoder.class, java.util.Objects.requireNonNull(encoder, "encoder"));
        return this;
    }

    private void configureLocalAuthentication() {
        AuthenticationManager manager = getSharedObject(AuthenticationManager.class);
        if (manager == null) {
            manager = new ResolversAuthenticationManager(List.of());
            setSharedObject(AuthenticationManager.class, manager);
        }
        if (!(manager instanceof AuthenticationResolverRegistry registry)) {
            return;
        }
        UserPrincipalService service = getObject(UserPrincipalService.class, null);
        PasswordEncoder encoder = getObject(PasswordEncoder.class, Pbkdf2Sha256PasswordEncoder::new);
        if (service != null && !registry.hasResolver(DaoAuthenticationResolver.class)) {
            registry.addResolver(new DaoAuthenticationResolver(service, encoder));
        }
        boolean passwordAuthentication = findConfigurer(HttpBasicConfigurer.class).isPresent()
                || findConfigurer(SubmitFormConfigurer.class).isPresent();
        for (var resolver : registry.getResolvers()) {
            if (resolver instanceof DaoAuthenticationResolver dao && dao.getPrincipalService() == null) {
                if (service != null) {
                    dao.setPrincipalService(service);
                    dao.setPasswordEncoder(encoder);
                } else if (passwordAuthentication) {
                    throw new IllegalStateException("UserPrincipalService is required for password authentication");
                }
            }
        }
        if (passwordAuthentication && registry.getResolvers().stream()
                .noneMatch(resolver -> resolver.supports(UsernamePasswordAuthentication.class))) {
            throw new IllegalStateException("An authentication resolver is required for username/password credentials");
        }
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

    public HttpSecurity csrf(Customizer<CsrfConfigurer<HttpSecurity>> customizer) {
        customizer.customize(attach(new CsrfConfigurer<>()));
        return this;
    }

    private void wireSessionAuthentication() {
        org.jmouse.security.web.session.SessionSettings settings =
                getSharedObject(org.jmouse.security.web.session.SessionSettings.class);
        boolean stateless = settings != null && settings.policy().isStateless();
        org.jmouse.security.web.session.SessionAuthenticateHandler strategy =
                getSharedObject(org.jmouse.security.web.session.SessionAuthenticateHandler.class);
        if (strategy == null) {
            strategy = stateless ? new org.jmouse.security.web.session.NullAuthenticatedSessionStrategy()
                    : new org.jmouse.security.web.session.ChangeSessionIdAuthenticateHandler();
        }
        org.jmouse.security.web.csrf.CsrfTokenRepository csrf =
                getSharedObject(org.jmouse.security.web.csrf.CsrfTokenRepository.class);
        if (csrf != null) {
            boolean allowCreate = settings == null || settings.policy().isAllowCreate();
            strategy = new org.jmouse.security.web.session.CompositeSessionAuthenticateHandler(strategy,
                    new org.jmouse.security.web.csrf.CsrfAuthenticationStrategy(csrf, allowCreate));
        }
        for (Filter filter : filters) {
            Filter delegate = filter instanceof OrderedFilter ordered ? ordered.filter() : filter;
            if (delegate instanceof org.jmouse.security.web.authentication.AbstractAuthenticationFilter authentication) {
                authentication.setSessionAuthenticationHandler(strategy);
            }
        }
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
        configureLocalAuthentication();
        configureConfigurers();
        wireSessionAuthentication();

        List<Filter> unique = new ArrayList<>(new LinkedHashSet<>(this.filters));
        unique.sort(java.util.Comparator.comparingInt(filter -> ((OrderedFilter) filter).order()));

        return new SecurityFilterChain(this.matcher, unique);
    }
}