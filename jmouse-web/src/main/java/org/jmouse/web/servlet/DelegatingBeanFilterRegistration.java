package org.jmouse.web.servlet;

import org.jmouse.beans.annotation.Ignore;
import org.jmouse.web.servlet.filter.DelegatingBeanFilter;
import org.jmouse.web.servlet.registration.FilterRegistrationBean;

@Ignore
public class DelegatingBeanFilterRegistration extends FilterRegistrationBean<DelegatingBeanFilter> {

    public DelegatingBeanFilterRegistration(String name) {
        super(getDelegatingFilterName(name), new DelegatingBeanFilter(name));
        setEnabled(true);
    }

    public static String getDelegatingFilterName(String name) {
        char[] chars = name.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        name = new String(chars);
        return "filter" + name;
    }

}
