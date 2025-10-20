package org.jmouse.security.core.access;

public enum Phase {
    BEFORE, AFTER;

    public boolean isBefore() {
        return this == BEFORE;
    }

    public boolean isAfter() {
        return this == AFTER;
    }

}
