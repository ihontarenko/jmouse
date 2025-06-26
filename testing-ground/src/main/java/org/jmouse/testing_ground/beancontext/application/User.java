package org.jmouse.testing_ground.beancontext.application;

import org.jmouse.beans.annotation.Bean;

@Bean(proxied = true)
public interface User {

    String getName();

}
