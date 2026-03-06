package org.jmouse.dom;

import java.util.Map;

public record FormMetadata(String action, String method, Map<String, String> hidden) {

    public static final String REQUEST_ATTRIBUTE = FormMetadata.class.getName() + ".request";

    public FormMetadata {
        hidden = hidden == null ? Map.of() : Map.copyOf(hidden);
    }

    public static FormMetadata of(String action, String method, Map<String, String> hidden) {
        return new FormMetadata(action, method, hidden);
    }

    public static FormMetadata of(String action, String method) {
        return of(action, method, Map.of());
    }

    public static FormMetadata of(String action) {
        return of(action, "POST", Map.of());
    }

}
