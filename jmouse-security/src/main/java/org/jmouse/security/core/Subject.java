package org.jmouse.security.core;

import java.util.Map;
import java.util.Set;

public interface Subject {

    String id();

    String kind();

    Map<String, Object> claims();

    Set<String> authorities();

}
