package org.jmouse.context.binding;

abstract public class AbstractBinder implements ObjectBinder {

    protected final BindContext context;

    public AbstractBinder(BindContext context) {
        this.context = context;
    }

}
