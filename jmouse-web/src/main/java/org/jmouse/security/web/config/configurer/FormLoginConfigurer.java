package org.jmouse.security.web.config.configurer;

import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.authentication.UsernameIdentityAuthentication;
import org.jmouse.security.web.OrderedFilter;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.authentication.UserIdentityAuthenticationFilter;
import org.jmouse.security.web.config.HttpSecurityBuilder;
import org.jmouse.security.web.config.SecurityConfigurer;

public class FormLoginConfigurer<B extends HttpSecurityBuilder<B>> implements SecurityConfigurer<B> {

    private String         loginUrl = "/login";
    private RequestMatcher matcher  = RequestMatcher.pathPattern(loginUrl);

    public FormLoginConfigurer<B> loginProcessingUrl(String url) {
        this.loginUrl = url;
        this.matcher = RequestMatcher.pathPattern(url);
        return this;
    }

    @Override
    public void configure(B http) {
        AuthenticationManager am = http.getSharedObject(AuthenticationManager.class);
        if (am == null) throw new IllegalStateException("AuthenticationManager is required");

        UserIdentityAuthenticationFilter filter = new UserIdentityAuthenticationFilter(am, matcher);

        http.addFilter(new OrderedFilter(filter, 150));
    }

}
