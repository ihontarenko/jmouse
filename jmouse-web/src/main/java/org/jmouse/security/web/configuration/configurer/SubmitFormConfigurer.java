package org.jmouse.security.web.configuration.configurer;

import jakarta.servlet.Filter;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.authentication.AuthenticationFailureHandler;
import org.jmouse.security.web.authentication.AuthenticationProvider;
import org.jmouse.security.web.authentication.AuthenticationSuccessHandler;
import org.jmouse.security.web.authentication.identity.SubmitFormRequestAuthenticationFilter;
import org.jmouse.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.jmouse.security.web.authentication.ui.FailureRedirectHandler;
import org.jmouse.security.web.authentication.ui.SuccessRedirectHandler;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.web.mvc.View;
import org.jmouse.web.mvc.ViewResolver;
import org.jmouse.web.mvc.view.internal.InternalView;
import org.jmouse.web.mvc.view.internal.InternalViewResolver;

import static org.jmouse.security.web.authentication.identity.SubmitFormRequestAuthenticationFilter.JMOUSE_USER_IDENTITY_PASSWORD;
import static org.jmouse.security.web.authentication.identity.SubmitFormRequestAuthenticationFilter.JMOUSE_USER_IDENTITY_USERNAME;

public class SubmitFormConfigurer<B extends HttpSecurityBuilder<B>>
        extends AbstractAuthenticationConfigurer<B, SubmitFormConfigurer<B>> {

    public static final String DEFAULT_LOGIN_PAGE = "/login";

    private AuthenticationProvider authenticationProvider;
    private String                 loginPage = DEFAULT_LOGIN_PAGE;
    private String                 usernameParameter;
    private String                 passwordParameter;

    public SubmitFormConfigurer<B> loginPage(String loginPage) {
        this.loginPage = loginPage;
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

    public SubmitFormConfigurer<B> authenticationProvider(AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
        return this;
    }

    @Override
    protected AuthenticationSuccessHandler defaultSuccessHandler() {
        return new SuccessRedirectHandler("/");
    }

    @Override
    protected AuthenticationFailureHandler defaultFailureHandler() {
        return new FailureRedirectHandler(loginPage);
    }

    @Override
    protected Filter doBuildFilter(
            AuthenticationManager authenticationManager,
            SecurityContextRepository repository,
            RequestMatcher matcher,
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

        ViewResolver viewResolver = http.getObject(InternalViewResolver.class);
        View         view         = viewResolver.resolveView("jmouse/login-form");

        http.addFilter(new DefaultLoginPageGeneratingFilter(
                view, loginPage,
                usernameParameter != null ? usernameParameter : JMOUSE_USER_IDENTITY_USERNAME,
                passwordParameter != null ? passwordParameter : JMOUSE_USER_IDENTITY_PASSWORD
        ));
    }

}
