package org.jmouse.security;

import java.util.Map;

public interface Resource {

    String id();

    String type();

    Map<String, Object> attributes();



}
