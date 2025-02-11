package test.application;

import org.jmouse.beans.annotation.Provide;

@Provide(proxied = true)
public interface User {

    String getName();

}
