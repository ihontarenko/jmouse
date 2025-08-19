package org.jmouse.web.mvc.servlet;

import org.jmouse.core.Priority;
import org.jmouse.web.mvc.DelegatingBeanFilterRegistration;

@Priority(-9999)
public class FirewallRequestFilterRegistration extends DelegatingBeanFilterRegistration {

    public FirewallRequestFilterRegistration() {
        super(FirewallRequestFilter.class);
    }

}
