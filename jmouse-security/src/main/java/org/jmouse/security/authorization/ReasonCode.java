package org.jmouse.security.authorization;

public interface ReasonCode {

    default boolean isGranted() {
        return false;
    }

}
