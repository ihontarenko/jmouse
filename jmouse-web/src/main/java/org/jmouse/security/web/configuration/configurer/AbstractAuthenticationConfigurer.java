package org.jmouse.security.web.configuration.configurer;

import jakarta.servlet.Filter;
import org.jmouse.core.Streamable;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.web.OrderedFilter;
import org.jmouse.security.web.authentication.*;
import org.jmouse.security.web.authentication.ui.LoginUrlAuthenticationEntryPoint;
import org.jmouse.security.web.authentication.ui.SuccessRedirectHandler;
import org.jmouse.security.web.configuration.Customizer;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.configuration.HttpSecurityConfigurer;
import org.jmouse.security.web.configuration.SharedAttributes;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.MappingMatcher;
import org.jmouse.web.match.routing.MatcherCriteria;

import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static org.jmouse.core.matcher.TextMatchers.notBlank;

/**
 * 🔧 AbstractAuthenticationConfigurer
 * <p>
 * Base configurer for username/password-style authentication with shared handler resolution
 * and a single filter registration.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>🔑 Resolve {@link AuthenticationManager} &amp; {@link SecurityContextRepository} from shared objects</li>
 *   <li>🧭 Resolve success/failure handlers (own → shared → defaults)</li>
 *   <li>🧱 Provide fluent setters for request matcher and handlers</li>
 *   <li>🏗️ Build and register an {@link OrderedFilter}</li>
 * </ul>
 *
 * @param <B> the concrete {@link HttpSecurityBuilder} type
 * @param <C> the concrete configurer type (for fluent API)
 */
@SuppressWarnings("unchecked")
public abstract class AbstractAuthenticationConfigurer<B extends HttpSecurityBuilder<B>, C extends AbstractAuthenticationConfigurer<B, C>>
        extends HttpSecurityConfigurer<C, B> {

    public static final String DEFAULT_LOGIN_PAGE = "/login";

    protected String                       loginPage                  = DEFAULT_LOGIN_PAGE;
    protected boolean                      continueChainBeforeSuccess = false;
    protected AuthenticationSuccessHandler successHandler;
    protected AuthenticationFailureHandler failureHandler;
    protected MappingMatcher<RequestRoute> mappingMatcher = MatcherCriteria.any();

    protected MappingMatcher<RequestRoute> processingUrl = MatcherCriteria.none();
    protected MappingMatcher<RequestRoute> submitFormUrl = MatcherCriteria.none();

    public C loginPage(String loginPage) {
        this.loginPage = loginPage;
        return (C) this;
    }

    /**
     * 🧭 Set a request matcher (e.g., path pattern) that triggers this auth flow.
     *
     * @param mappingMatcher matcher to activate authentication
     * @return this configurer
     */
    public C requestMatcher(MappingMatcher<RequestRoute> mappingMatcher) {
        this.mappingMatcher = mappingMatcher;
        return (C) this;
    }

    /**
     * 🧭 Convenience for one or more path patterns (OR-ed together).
     *
     * @param patterns path patterns to match
     * @return this configurer
     */
    public C requestMatcher(String... patterns) {
        Streamable.of(patterns)
                .filter(Objects::nonNull)
                .filter(notBlank()::matches);

        return (C) this;
    }

    public C anyRequest() {
        return requestMatcher(MatcherCriteria.any());
    }

    public C redirect(Customizer<RedirectHandlerConfigurer> customizer) {
        RedirectHandlerConfigurer configurer = new RedirectHandlerConfigurer();
        customizer.customize(configurer);
        return (C) this;
    }

    /**
     * ✅ Provide a custom success handler (falls back to shared/defaults).
     *
     * @param h success handler
     * @return this configurer
     */
    public C successHandler(AuthenticationSuccessHandler h) {
        this.successHandler = requireNonNull(h);
        return (C) this;
    }

    /**
     * ❌ Provide a custom failure handler (falls back to shared/defaults).
     *
     * @param h failure handler
     * @return this configurer
     */
    public C failureHandler(AuthenticationFailureHandler h) {
        this.failureHandler = requireNonNull(h);
        return (C) this;
    }

    public C continueChainBeforeSuccess(boolean flag) {
        this.continueChainBeforeSuccess = flag;
        return (C) this;
    }

    /**
     * 🔧 Initialization step.
     * <br>
     * Called once before the actual {@link #configure(B builder)} phase.
     * Use this for setting defaults or preparing shared state.
     *
     * @param builder the security builder
     * @throws Exception in case of setup errors
     */
    @Override
    @SuppressWarnings("unchecked")
    public void initialize(B builder) throws Exception {
        if (builder.getConfigurer(ExceptionHandlingConfigurer.class)
                instanceof ExceptionHandlingConfigurer<?> exceptionHandlingConfigurer
        ) {
            exceptionHandlingConfigurer.authenticationEntryPoint(
                    new LoginUrlAuthenticationEntryPoint(loginPage)
            );
        }
    }

    /**
     * 🔩 Template method: resolve shared objects/handlers, build and register the filter.
     */
    @Override
    public void configure(B http) {
        AuthenticationManager     authenticationManager = http.getSharedObject(SharedAttributes.AUTHENTICATION_MANAGER);
        SecurityContextRepository repository            = http.getSharedObject(SharedAttributes.CONTEXT_REPOSITORY);
        MatcherCriteria           matcherCriteria       = resolveMatcher();

        if (matcherCriteria == null) {
            throw new IllegalStateException("MATCHER-CRITERIA must be set (use processing() or requestMatcher(...))");
        }

        AuthenticationSuccessHandler successHandler = resolveSuccessHandler(http);
        AuthenticationFailureHandler failureHandler = resolveFailureHandler(http);

        // Keep shared in sync so downstream configurers/filters can reuse unified handlers
        http.setSharedObject(SharedAttributes.SUCCESS_HANDLER, successHandler);
        http.setSharedObject(SharedAttributes.FAILURE_HANDLER, failureHandler);

        if (doBuildFilter(authenticationManager, repository, matcherCriteria, successHandler, failureHandler)
                instanceof AbstractAuthenticationFilter authenticationFilter) {
            authenticationFilter.setContinueChainBeforeSuccess(continueChainBeforeSuccess);
            http.addFilter(authenticationFilter);
        }
    }

    /**
     * 🏗️ Build the concrete authentication filter for this configurer.
     *
     * @param authenticationManager auth manager to use
     * @param repository            context repository to persist security context
     * @param matcher               request matcher to trigger authentication
     * @param successHandler        success handler
     * @param failureHandler        failure handler
     * @return a configured {@link Filter} (typically an {@link OrderedFilter})
     */
    protected abstract Filter doBuildFilter(
            AuthenticationManager authenticationManager,
            SecurityContextRepository repository, MatcherCriteria matcher,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler
    );

    /**
     * 🟢 Default success: no-op 200 OK (override if desired).
     */
    protected AuthenticationSuccessHandler defaultSuccessHandler() {
        return new NoopHttp200SuccessHandler();
    }

    /**
     * 🔴 Default failure: no-op 401 Unauthorized (override if desired).
     */
    protected AuthenticationFailureHandler defaultFailureHandler() {
        return new NoopHttp401FailureHandler();
    }

    /**
     * 🔎 Resolve the configured request matcher (must not be {@code null} at configure time).
     */
    private MatcherCriteria resolveMatcher() {
        return (MatcherCriteria) this.mappingMatcher;
    }

    /**
     * 🔎 Resolve success handler: own → shared → default.
     */
    private AuthenticationSuccessHandler resolveSuccessHandler(B http) {
        if (this.successHandler != null) return this.successHandler;
        AuthenticationSuccessHandler shared = http.getSharedObject(SharedAttributes.SUCCESS_HANDLER);
        return shared != null ? shared : defaultSuccessHandler();
    }

    /**
     * 🔎 Resolve failure handler: own → shared → default.
     */
    private AuthenticationFailureHandler resolveFailureHandler(B http) {
        if (this.failureHandler != null) return this.failureHandler;
        AuthenticationFailureHandler shared = http.getSharedObject(SharedAttributes.FAILURE_HANDLER);
        return shared != null ? shared : defaultFailureHandler();
    }

    public class RedirectHandlerConfigurer {

        public void url(String redirectUrl) {
            successHandler(new SuccessRedirectHandler(redirectUrl));
        }

    }

}
