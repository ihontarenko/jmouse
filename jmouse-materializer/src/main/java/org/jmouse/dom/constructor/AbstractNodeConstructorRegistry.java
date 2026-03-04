package org.jmouse.dom.constructor;

import java.util.HashMap;
import java.util.Map;

abstract public class AbstractNodeConstructorRegistry implements NodeConstructorRegistry {

    private final Map<Class<?>, NodeConstructor<?>> builders = new HashMap<>();

    abstract protected void initialize();

    @Override
    public <T> void setConstructor(Class<T> classType, NodeConstructor<? extends T> constructor) {
        this.builders.put(classType, constructor);
    }

    @Override
    public <T> NodeConstructor<T> getConstructor(Class<T> type) {
        return (NodeConstructor<T>) builders.get(type);
    }

}
