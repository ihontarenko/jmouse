package org.jmouse.web.servlet.filter;

import org.jmouse.mvc.DelegatingBeanFilterRegistration;
import org.jmouse.util.Priority;

@Priority(-9999)
public class MultipartRequestFilterRegistration extends DelegatingBeanFilterRegistration {

    public MultipartRequestFilterRegistration() {
        super(MultipartRequestFilter.class);
    }

}
