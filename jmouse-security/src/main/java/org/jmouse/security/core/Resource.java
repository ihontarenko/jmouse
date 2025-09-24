package org.jmouse.security.core;

import java.util.Map;

public interface Resource {

    String id();

    String type();

    Map<String, Object> attributes();



}
