package org.jmouse.context.bind;

public class AbstractCallback implements BindCallback {

    protected BindCallback parent;

    protected AbstractCallback(BindCallback parent) {
        this.parent = parent;
    }

    protected AbstractCallback() {
        this(NULL);
    }

}
