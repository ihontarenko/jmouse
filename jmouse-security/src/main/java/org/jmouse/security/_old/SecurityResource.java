package org.jmouse.security._old;

import java.util.Map;

public record SecurityResource(String id, String type, Map<String, Object> attributes) implements Resource {

    public static Resource of(String id, String type, Map<String, Object> attributes) {
        return new SecurityResource(id, type, attributes);
    }

}
