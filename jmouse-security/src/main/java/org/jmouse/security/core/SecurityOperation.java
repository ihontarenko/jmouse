package org.jmouse.security.core;

import java.util.Map;

public record SecurityOperation(String verb, Map<String, Object> attributes) implements Operation {

    public static Operation of(String verb, Map<String, Object> attributes) {
        return new SecurityOperation(verb, attributes);
    }

    /**
     * "READ", "WRITE", "INVOKE", "HTTP:GET", "RPC:Call"
     */
    @Override
    public String verb() {
        return verb;
    }

}
