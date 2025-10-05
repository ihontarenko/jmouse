package org.jmouse.security.web.configuration;

import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.security.web.configuration.builder.HttpSecurity;

@BeanFactories
public class SecurityConfiguration {

    public HttpSecurity httpSecurity() {
        HttpSecurity httpSecurity = new HttpSecurity();



        return httpSecurity;
    }

}
