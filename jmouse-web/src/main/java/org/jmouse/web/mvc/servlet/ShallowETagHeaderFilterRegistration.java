package org.jmouse.web.mvc.servlet;

import org.jmouse.core.Priority;
import org.jmouse.web.mvc.DelegatingBeanFilterRegistration;

@Priority(-5000)
public class ShallowETagHeaderFilterRegistration extends DelegatingBeanFilterRegistration {

    public ShallowETagHeaderFilterRegistration() {
        super(ShallowETagHeaderFilter.class);
    }

}
