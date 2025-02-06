package org.jmouse.common.dom.builder;

public interface NodeBuilderRegistry {

    <T> void setBuilder(Class<T> classType, NodeBuilder<? extends T> builder);

    <T> NodeBuilder<T> getBuilder(Class<T> classType);

}
