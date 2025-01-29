package test.application;

import svit.beans.annotation.Provide;

@Provide(proxied = true)
public interface User {

    String getName();

}
