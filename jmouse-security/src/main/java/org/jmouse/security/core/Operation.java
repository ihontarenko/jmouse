package org.jmouse.security.core;

import java.util.Map;

public interface Operation {

    /**
     * "READ", "WRITE", "INVOKE", "HTTP:GET", "RPC:Call"
     */
    String verb();

    Map<String, Object> attributes();

}
