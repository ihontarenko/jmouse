package org.jmouse.web.mvc.servlet;

import org.jmouse.core.Priority;
import org.jmouse.web.mvc.DelegatingBeanFilterRegistration;

@Priority(-5000)
public class ShallowEtagHeaderFilterRegistration extends DelegatingBeanFilterRegistration {

    public ShallowEtagHeaderFilterRegistration() {
        super(ShallowEtagHeaderFilter.class);
    }

}
