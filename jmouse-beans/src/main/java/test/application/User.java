package test.application;

import org.jmouse.testing_ground.beans.annotation.Provide;

@Provide(proxied = true)
public interface User {

    String getName();

}
