package org.jmouse.dom.template.model;

public record ControlState(
        Object value,
        boolean hasError,
        String errorMessage
) {

    public static ControlState empty() {
        return new ControlState(null, false, null);
    }

    public static ControlState invalid(String message, Object value) {
        return new ControlState(value, true, message);
    }

    public static ControlState valid(Object value) {
        return new ControlState(value, false, null);
    }
}
