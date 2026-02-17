package org.jmouse.common.dom.old_builder;

import java.util.HashMap;
import java.util.Map;

abstract public class AbstractBuilderRegistry implements NodeBuilderRegistry {

    private final Map<Class<?>, NodeBuilder<?>> builders = new HashMap<>();

    abstract protected void initialize();

    @Override
    public <T> void setBuilder(Class<T> classType, NodeBuilder<? extends T> builder) {
        this.builders.put(classType, builder);
    }

    @Override
    public <T> NodeBuilder<T> getBuilder(Class<T> classType) {
        return (NodeBuilder<T>) builders.get(classType);
    }

}
