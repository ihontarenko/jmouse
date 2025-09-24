package org.jmouse.core.proxy;

public final class Bubble extends RuntimeException {

    public Bubble(Throwable cause) {
        super(null, cause, false, false);
    }

    @Override
    public synchronized Throwable getCause() {
        return super.getCause();
    }

}