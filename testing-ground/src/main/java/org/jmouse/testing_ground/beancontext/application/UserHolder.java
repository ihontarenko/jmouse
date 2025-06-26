package org.jmouse.testing_ground.beancontext.application;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.Qualifier;

@Bean
public record UserHolder(@Qualifier("client") User user) {
}
