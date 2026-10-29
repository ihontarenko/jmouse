package org.jmouse.security.web.configuration.configurer;

import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.configuration.HttpSecurityConfigurer;
import org.jmouse.security.web.csrf.CsrfFilter;
import org.jmouse.security.web.csrf.CsrfTokenRepository;
import org.jmouse.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.jmouse.security.web.session.SessionCreationPolicy;
import org.jmouse.security.web.session.SessionSettings;

public final class CsrfConfigurer<B extends HttpSecurityBuilder<B>> extends HttpSecurityConfigurer<CsrfConfigurer<B>, B> {
    private boolean enabled = true;
    private CsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();

    public CsrfConfigurer<B> disable() {
        enabled = false;
        return this;
    }

    public CsrfConfigurer<B> repository(CsrfTokenRepository repository) {
        this.repository = java.util.Objects.requireNonNull(repository, "repository");
        return this;
    }

    @Override
    public void configure(B http) {
        if (!enabled) {
            return;
        }
        SessionSettings settings = http.getSharedObject(SessionSettings.class);
        SessionCreationPolicy policy = settings == null ? SessionCreationPolicy.IF_REQUIRED : settings.policy();
        if (policy.isStateless() && repository.requiresSession()) {
            throw new IllegalStateException("Choose a stateless CSRF repository or explicitly disable CSRF for a non-cookie API");
        }
        http.setSharedObject(CsrfTokenRepository.class, repository);
        http.addFilter(new CsrfFilter(repository, policy));
    }
}
