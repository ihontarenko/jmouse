package org.jmouse.web.servlet.filter;

import org.jmouse.web.mvc.DelegatingBeanFilterRegistration;
import org.jmouse.core.Priority;

@Priority(-9999)
public class MultipartRequestFilterRegistration extends DelegatingBeanFilterRegistration {

    public MultipartRequestFilterRegistration() {
        super(MultipartRequestFilter.class);
    }

}
