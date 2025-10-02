package org.jmouse.security.web.configuration;

import org.jmouse.security.web.SecurityFilterChain;

public class HttpSecurityConfigurer<T extends HttpSecurityConfigurer<T, B>, B extends HttpSecurityBuilder<B>>
        extends ConfigurerAdapter<SecurityFilterChain, B> {

    @SuppressWarnings("unchecked")
    public B disable() {
        B builder = getBuilder();
        builder.removeConfigurer(getClass());
        return builder;
    }

}
