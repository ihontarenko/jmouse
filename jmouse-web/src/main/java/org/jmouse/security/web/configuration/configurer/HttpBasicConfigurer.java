package org.jmouse.security.web.configuration.configurer;

import jakarta.servlet.Filter;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.web.authentication.*;
import org.jmouse.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.jmouse.security.web.authentication.www.BasicAuthenticationFilter;
import org.jmouse.security.web.context.SecurityContextRepository;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.web.match.routing.MatcherCriteria;

public class HttpBasicConfigurer<B extends HttpSecurityBuilder<B>>
        extends AbstractAuthenticationConfigurer<B, HttpBasicConfigurer<B>> {

    private final BasicAuthenticationEntryPoint entryPoint         = new BasicAuthenticationEntryPoint();
    private       boolean                       challengeOnFailure = true;

    public HttpBasicConfigurer<B> realmName(String realm) {
        this.entryPoint.setRealmName(realm);
        return this;
    }

    public HttpBasicConfigurer<B> disableChallengeOnFailure() {
        this.challengeOnFailure = false;
        return this;
    }

    public HttpBasicConfigurer<B> enableChallengeOnFailure() {
        this.challengeOnFailure = true;
        return this;
    }

    @Override
    protected Filter doBuildFilter(
            AuthenticationManager authenticationManager,
            SecurityContextRepository repository,
            MatcherCriteria matcherCriteria,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler
    ) {
        AuthenticationFailureHandler failure = (failureHandler != null)
                ? failureHandler
                : new NoopHttp401FailureHandler();

        AuthenticationSuccessHandler success = (successHandler != null)
                ? successHandler
                : new NoopHttp200SuccessHandler();

        if (challengeOnFailure) {
            failure = entryPoint::initiate;
        }

        return new BasicAuthenticationFilter(authenticationManager, repository, matcherCriteria, success, failure);
    }

}
