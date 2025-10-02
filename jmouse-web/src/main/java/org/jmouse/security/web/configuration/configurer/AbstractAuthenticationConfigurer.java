package org.jmouse.security.web.configuration.configurer;

import jakarta.servlet.Filter;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.web.OrderedFilter;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.authentication.AuthenticationFailureHandler;
import org.jmouse.security.web.authentication.AuthenticationSuccessHandler;
import org.jmouse.security.web.authentication.NoopHttp200SuccessHandler;
import org.jmouse.security.web.authentication.NoopHttp401FailureHandler;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.configuration.SecurityConfigurer;
import org.jmouse.security.web.configuration.SharedAttributes;
import org.jmouse.security.web.context.SecurityContextRepository;

import static java.util.Objects.requireNonNull;

/**
 * 🔧 Base configurer for username/password style authentication with
 * shared handler resolution and a single filter registration.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Resolve {@code AuthenticationManager} & {@code SecurityContextRepository} from shared objects</li>
 *   <li>Resolve success/failure handlers (own → shared → defaults)</li>
 *   <li>Expose fluent setters for handlers and matcher</li>
 *   <li>Build and register an {@link OrderedFilter}</li>
 * </ul>
 */
public abstract class AbstractAuthenticationConfigurer<B extends HttpSecurityBuilder<B>>
        implements SecurityConfigurer<B> {

    private RequestMatcher               matcher;
    private AuthenticationSuccessHandler successHandler;
    private AuthenticationFailureHandler failureHandler;

    /**
     * Set a request matcher (e.g. path pattern) that triggers this authn flow.
     */
    public AbstractAuthenticationConfigurer<B> requestMatcher(RequestMatcher m) {
        this.matcher = requireNonNull(m);
        return this;
    }

    /**
     * Shortcut for common case when matcher is a path pattern.
     */
    public AbstractAuthenticationConfigurer<B> loginProcessingUrl(String url) {
        return requestMatcher(RequestMatcher.pathPattern(requireNonNull(url)));
    }

    /**
     * Provide a custom success handler (falls back to shared/defaults).
     */
    public AbstractAuthenticationConfigurer<B> successHandler(AuthenticationSuccessHandler h) {
        this.successHandler = requireNonNull(h);
        return this;
    }

    /**
     * Provide a custom failure handler (falls back to shared/defaults).
     */
    public AbstractAuthenticationConfigurer<B> failureHandler(AuthenticationFailureHandler h) {
        this.failureHandler = requireNonNull(h);
        return this;
    }

    @Override
    public final void configure(B http) {
        AuthenticationManager     authenticationManager = http.getSharedObject(SharedAttributes.AUTHENTICATION_MANAGER);
        SecurityContextRepository repository            = http.getSharedObject(SharedAttributes.CONTEXT_REPOSITORY);

        RequestMatcher m = requireNonNull(resolveMatcher(),
                                          "RequestMatcher must be set (use loginProcessingUrl(...) or requestMatcher(...))");

        AuthenticationSuccessHandler successHandler = resolveSuccessHandler(http);
        AuthenticationFailureHandler failureHandler = resolveFailureHandler(http);

        // keep shared in sync so downstream configurers/filters can reuse unified handlers
        http.setSharedObject(SharedAttributes.SUCCESS_HANDLER, successHandler);
        http.setSharedObject(SharedAttributes.FAILURE_HANDLER, failureHandler);

        var filter = buildFilter(authenticationManager, repository, m, successHandler, failureHandler);
        http.addFilter(filter);
    }

    /**
     * Build the concrete authentication filter for this configurer.
     */
    protected abstract Filter buildFilter(AuthenticationManager authenticationManager, SecurityContextRepository repository, RequestMatcher matcher, AuthenticationSuccessHandler successHandler, AuthenticationFailureHandler failureHandler);

    /**
     * Default: Noop 200 on success; override in subclasses if desired.
     */
    protected AuthenticationSuccessHandler defaultSuccessHandler() {
        return new NoopHttp200SuccessHandler();
    }

    /**
     * Default: Noop 401 on failure; override in subclasses if desired.
     */
    protected AuthenticationFailureHandler defaultFailureHandler() {
        return new NoopHttp401FailureHandler();
    }

    private RequestMatcher resolveMatcher() {
        return this.matcher;
    }

    private AuthenticationSuccessHandler resolveSuccessHandler(B http) {
        if (this.successHandler != null) {
            return this.successHandler;
        }

        AuthenticationSuccessHandler shared = http.getSharedObject(SharedAttributes.SUCCESS_HANDLER);

        return shared != null ? shared : defaultSuccessHandler();
    }

    private AuthenticationFailureHandler resolveFailureHandler(B http) {
        if (this.failureHandler != null) {
            return this.failureHandler;
        }

        AuthenticationFailureHandler shared = http.getSharedObject(SharedAttributes.FAILURE_HANDLER);

        return shared != null ? shared : defaultFailureHandler();
    }
}