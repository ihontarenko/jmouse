package org.jmouse.security.web.configuration.configurer;

import jakarta.servlet.Filter;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.web.authentication.AuthenticationFailureHandler;
import org.jmouse.security.web.authentication.AuthenticationProvider;
import org.jmouse.security.web.authentication.AuthenticationSuccessHandler;
import org.jmouse.security.web.authentication.form.SubmitFormRequestAuthenticationFilter;
import org.jmouse.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.jmouse.security.web.authentication.ui.FailureRedirectHandler;
import org.jmouse.security.web.authentication.ui.SavedRequestAwareAuthenticationSuccessHandler;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.configuration.SharedAttributes;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.cache.HttpSessionRequestCache;
import org.jmouse.web.http.cache.RequestCache;
import org.jmouse.web.match.routing.MatcherCriteria;
import org.jmouse.web.mvc.View;
import org.jmouse.web.mvc.ViewResolver;
import org.jmouse.web.mvc.view.internal.InternalViewResolver;

import static org.jmouse.security.web.authentication.form.SubmitFormRequestAuthenticationFilter.JMOUSE_USER_IDENTITY_PASSWORD;
import static org.jmouse.security.web.authentication.form.SubmitFormRequestAuthenticationFilter.JMOUSE_USER_IDENTITY_USERNAME;

public class SubmitFormConfigurer<B extends HttpSecurityBuilder<B>>
        extends AbstractAuthenticationConfigurer<B, SubmitFormConfigurer<B>> {

    private AuthenticationProvider authenticationProvider;
    private boolean                generateDefaultLoginPage = true;
    private String                 usernameParameter;
    private String                 passwordParameter;

    public SubmitFormConfigurer<B> disableDefaultLoginPage() {
        this.generateDefaultLoginPage = false;
        return this;
    }

    public SubmitFormConfigurer<B> usernamePassword(String usernameParameter, String passwordParameter) {
        return usernameParameter(usernameParameter).passwordParameter(passwordParameter);
    }

    public SubmitFormConfigurer<B> usernameParameter(String usernameParameter) {
        this.usernameParameter = usernameParameter;
        return this;
    }

    public SubmitFormConfigurer<B> passwordParameter(String passwordParameter) {
        this.passwordParameter = passwordParameter;
        return this;
    }

    public ProcessingURLConfigurer processing() {
        return new ProcessingURLConfigurer();
    }

    public SubmitFormConfigurer<B> authenticationProvider(AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
        return this;
    }

    @Override
    protected AuthenticationSuccessHandler defaultSuccessHandler() {
        RequestCache requestCache = getBuilder().getSharedObject(SharedAttributes.REQUEST_CACHE);

        if (requestCache == null) {
            requestCache = new HttpSessionRequestCache();
        }

        return new SavedRequestAwareAuthenticationSuccessHandler(requestCache).defaultTargetUrl("/");
    }

    @Override
    protected AuthenticationFailureHandler defaultFailureHandler() {
        return new FailureRedirectHandler(loginPage);
    }

    @Override
    protected Filter doBuildFilter(
            AuthenticationManager authenticationManager,
            SecurityContextRepository repository,
            MatcherCriteria matcher,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler
    ) {
        SubmitFormRequestAuthenticationFilter filter = new SubmitFormRequestAuthenticationFilter(
                authenticationManager, repository, matcher, successHandler, failureHandler);

        filter.setAuthenticationProvider(authenticationProvider);
        filter.setUsernameParameter(usernameParameter);
        filter.setPasswordParameter(passwordParameter);

        return filter;
    }

    @Override
    public void configure(B http) {
        super.configure(http);

        if (!generateDefaultLoginPage) {
            return;
        }

        ViewResolver viewResolver = http.getObject(InternalViewResolver.class);
        View         view         = viewResolver.resolveView("jmouse/login-form");

        http.addFilter(new DefaultLoginPageGeneratingFilter(
                view, loginPage,
                usernameParameter != null ? usernameParameter : JMOUSE_USER_IDENTITY_USERNAME,
                passwordParameter != null ? passwordParameter : JMOUSE_USER_IDENTITY_PASSWORD
        ));
    }

    /**
     * 🛣️ Builder for common "processing URL + method" configuration.
     */
    public class ProcessingURLConfigurer {

        private final MatcherCriteria matcherCriteria = new MatcherCriteria();

        /**
         * 🧭 Define the form/action path pattern to match.
         *
         * @param url action or endpoint path pattern
         * @return this builder
         */
        public ProcessingURLConfigurer formAction(String url) {
            matcherCriteria.pathPattern(url);
            return this;
        }

        /**
         * 🔁 Constrain the matcher to a specific HTTP method.
         *
         * <p>⚠️ Requires {@link #formAction(String)} to be called first,
         * otherwise no base path matcher is defined.</p>
         *
         * @param httpMethod HTTP method to match (e.g. GET, POST)
         * @return parent configurer
         * @throws IllegalStateException if {@code formAction(...)} was not set
         */
        public SubmitFormConfigurer<B> httpMethod(HttpMethod httpMethod) {
            if (matcherCriteria.getMatchers().isEmpty()) {
                throw new IllegalStateException("formAction(...) must be set before httpMethod(...)");
            }

            return SubmitFormConfigurer.this.requestMatcher(
                    matcherCriteria.httpMethod(httpMethod)
            );
        }

        /**
         * ⬆️ Shortcut for {@code GET}.
         */
        public SubmitFormConfigurer<B> get() {
            return httpMethod(HttpMethod.GET);
        }

        /**
         * ⬇️ Shortcut for {@code POST}.
         */
        public SubmitFormConfigurer<B> post() {
            return httpMethod(HttpMethod.POST);
        }
    }

}
