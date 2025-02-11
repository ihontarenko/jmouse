package test.application;

import org.jmouse.testing_ground.beans.annotation.Provide;
import org.jmouse.testing_ground.beans.annotation.Qualifier;

@Provide
public record UserHolder(@Qualifier("client") User user) {
}
