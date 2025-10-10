package org.jmouse.security.web.configuration.configurer;

import org.jmouse.security.web.authentication.AnonymousAuthenticationFilter;
import org.jmouse.security.web.configuration.HttpSecurityBuilder;
import org.jmouse.security.web.configuration.HttpSecurityConfigurer;

final public class AnonymousConfigurer<B extends HttpSecurityBuilder<B>>
        extends HttpSecurityConfigurer<AnonymousConfigurer<B>, B> {

    @Override
    public void configure(B builder) throws Exception {
        builder.addFilter(new AnonymousAuthenticationFilter());
    }

}
