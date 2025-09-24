package org.jmouse.security.core;

public final class Subjects {

    public static Subject anonymous() {
        return new Anonymous();
    }

}
