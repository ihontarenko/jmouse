package org.jmouse.security;

import java.util.Map;

public interface CredentialCarrier {

    String first(String key);

    Map<String, String> all();

}
