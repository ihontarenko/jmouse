package org.jmouse.security.core;

import java.util.Map;

public interface CredentialCarrier {

    String first(String key);

    Map<String, String> all();

}
