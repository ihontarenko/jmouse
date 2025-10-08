package org.jmouse.security.web.configuration.configurer;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.Streamable;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.web.OrderedFilter;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.authentication.AuthenticationFailureHandler;
import org.jmouse.security.web.authentication.AuthenticationSuccessHandler;
import org.jmouse.security.web.authentication.NoopHttp200SuccessHandler;
import org.jmouse.security.web.authentication.NoopHttp401FailureHandler;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.configuration.HttpSecurityConfigurer;
import org.jmouse.security.web.configuration.SharedAttributes;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.web.http.HttpMethod;

import static java.util.Objects.requireNonNull;

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

    private RequestMatcher               requestMatcher = RequestMatcher.any();
    private AuthenticationSuccessHandler successHandler;
    private AuthenticationFailureHandler failureHandler;

    /**
     * 🧭 Set a request matcher (e.g., path pattern) that triggers this auth flow.
     *
     * @param requestMatcher matcher to activate authentication
     * @return this configurer
     */
    public C requestMatcher(RequestMatcher requestMatcher) {
        this.requestMatcher = requireNonNull(requestMatcher);
        return (C) this;
    }

    /**
     * 🧭 Convenience for one or more path patterns (OR-ed together).
     *
     * @param patterns path patterns to match
     * @return this configurer
     */
    public C requestMatcher(String... patterns) {
        Matcher<HttpServletRequest> requestMatcher = Streamable.of(patterns)
                .map(RequestMatcher::pathPattern)
                .map(Matcher::narrow)
                .reduce(Matcher.constant(false), Matcher::logicalOr);
        this.requestMatcher = requestMatcher::matches;
        return (C) this;
    }

    public C anyRequest() {
        return requestMatcher(RequestMatcher.any());
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

    /**
     * 🔩 Template method: resolve shared objects/handlers, build and register the filter.
     */
    @Override
    public void configure(B http) {
        AuthenticationManager     authenticationManager = http.getSharedObject(SharedAttributes.AUTHENTICATION_MANAGER);
        SecurityContextRepository repository            = http.getSharedObject(SharedAttributes.CONTEXT_REPOSITORY);
        RequestMatcher            matcher               = resolveMatcher();

        if (matcher == null) {
            throw new IllegalStateException(
                    "REQUEST-MATCHER must be set (use processing() or requestMatcher(...))");
        }

        AuthenticationSuccessHandler successHandler = resolveSuccessHandler(http);
        AuthenticationFailureHandler failureHandler = resolveFailureHandler(http);

        // Keep shared in sync so downstream configurers/filters can reuse unified handlers
        http.setSharedObject(SharedAttributes.SUCCESS_HANDLER, successHandler);
        http.setSharedObject(SharedAttributes.FAILURE_HANDLER, failureHandler);

        http.addFilter(doBuildFilter(authenticationManager, repository, requestMatcher, successHandler, failureHandler));
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
            SecurityContextRepository repository, RequestMatcher matcher,
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
    private RequestMatcher resolveMatcher() {
        return this.requestMatcher;
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

}
