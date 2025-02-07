package org.jmouse.core.bind;

public class AbstractCallback implements BindCallback {

    protected BindCallback parent;

    protected AbstractCallback(BindCallback parent) {
        this.parent = parent;
    }

    protected AbstractCallback() {
        this(NOOP);
    }

}
