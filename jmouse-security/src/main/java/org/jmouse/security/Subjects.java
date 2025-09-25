package org.jmouse.security;

public final class Subjects {

    public static Subject anonymous() {
        return new Anonymous();
    }

}
