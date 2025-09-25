package org.jmouse.security._old;

import java.util.Map;

public interface CredentialCarrier {

    String first(String key);

    Map<String, String> all();

}
