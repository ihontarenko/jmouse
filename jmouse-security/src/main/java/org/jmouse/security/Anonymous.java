package org.jmouse.security;

import java.util.Map;
import java.util.Set;

public class Anonymous implements Subject {

    public static final String ANONYMOUS = "ANONYMOUS";

    @Override
    public String id() {
        return ANONYMOUS;
    }

    @Override
    public String kind() {
        return ANONYMOUS;
    }

    @Override
    public Map<String, Object> claims() {
        return Map.of();
    }

    @Override
    public Set<String> authorities() {
        return Set.of();
    }

}
