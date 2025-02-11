package test.application;

import org.jmouse.beans.annotation.Provide;
import org.jmouse.beans.annotation.Qualifier;

@Provide
public record UserHolder(@Qualifier("client") User user) {
}
