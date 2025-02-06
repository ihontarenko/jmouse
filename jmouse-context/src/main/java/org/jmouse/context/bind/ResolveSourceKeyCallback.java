package org.jmouse.context.bind;

public class ResolveSourceKeyCallback implements BindCallback {

    @Override
    public NamePath onResolveKey(NamePath name) {
        return BindCallback.super.onResolveKey(name);
    }

}
