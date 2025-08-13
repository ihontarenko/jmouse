package org.jmouse.web.servlet.filter;

import org.jmouse.mvc.DelegatingBeanFilterRegistration;
import org.jmouse.util.Priority;

@Priority(-10000)
public class RequestWrapperFilterRegistration extends DelegatingBeanFilterRegistration {

    public RequestWrapperFilterRegistration() {
        super(RequestWrapperFilter.class);
    }

}
