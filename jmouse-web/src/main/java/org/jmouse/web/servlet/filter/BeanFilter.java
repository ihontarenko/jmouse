package org.jmouse.web.servlet.filter;

import jakarta.servlet.Filter;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.web.context.WebBeanContext;

public interface BeanFilter extends Filter, InitializingBeanSupport<WebBeanContext> {
    
}
