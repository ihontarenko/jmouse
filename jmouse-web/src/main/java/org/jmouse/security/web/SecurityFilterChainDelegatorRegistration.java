package org.jmouse.security.web;

import org.jmouse.core.Priority;
import org.jmouse.web.mvc.DelegatingBeanFilterRegistration;

@Priority(-10000)
public class SecurityFilterChainDelegatorRegistration extends DelegatingBeanFilterRegistration {

    public SecurityFilterChainDelegatorRegistration() {
        super(SecurityFilterChainDelegator.class);
    }

}
