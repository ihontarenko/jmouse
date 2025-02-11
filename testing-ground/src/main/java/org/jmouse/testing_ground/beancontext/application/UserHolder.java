package org.jmouse.testing_ground.beancontext.application;

import org.jmouse.beans.annotation.Provide;
import org.jmouse.beans.annotation.Qualifier;

@Provide
public record UserHolder(@Qualifier("client") User user) {
}
