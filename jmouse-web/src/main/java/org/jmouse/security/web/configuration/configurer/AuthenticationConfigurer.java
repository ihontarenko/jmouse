package org.jmouse.security.web.configuration.configurer;

import org.jmouse.security.web.MatchableSecurityFilterChain;
import org.jmouse.security.web.configuration.ConfigurerAdapter;
import org.jmouse.core.Customizer;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.configuration.HttpSecurityConfigurer;
import org.jmouse.security.web.configuration.builder.HttpSecurity;

public class AuthenticationConfigurer<B extends HttpSecurityBuilder<B>>
        extends HttpSecurityConfigurer<AuthenticationConfigurer<B>, B> {

    private final Attacher attacher;

    public AuthenticationConfigurer(Attacher attacher) {
        this.attacher = attacher;
    }

    public AuthenticationConfigurer<B> anonymous(Customizer<AnonymousConfigurer<HttpSecurity>> customizer) {
        customizer.customize(attacher.attach(new AnonymousConfigurer<>()));
        return this;
    }

    public AuthenticationConfigurer<B> submitForm(Customizer<SubmitFormConfigurer<HttpSecurity>> customizer) {
        customizer.customize(attacher.attach(new SubmitFormConfigurer<>()));
        return this;
    }

    public AuthenticationConfigurer<B> httpBasic(Customizer<HttpBasicConfigurer<HttpSecurity>> customizer) {
        customizer.customize(attacher.attach(new HttpBasicConfigurer<>()));
        return this;
    }

    public AuthenticationConfigurer<B> jwt(Customizer<JwtTokenConfigurer<HttpSecurity>> customizer) {
        customizer.customize(attacher.attach(new JwtTokenConfigurer<>()));
        return this;
    }

    @FunctionalInterface
    public interface Attacher {
        <C extends ConfigurerAdapter<MatchableSecurityFilterChain, HttpSecurity>> C attach(C configurer);
    }

}
