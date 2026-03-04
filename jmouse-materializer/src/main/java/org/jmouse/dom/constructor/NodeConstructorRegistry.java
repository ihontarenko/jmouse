package org.jmouse.dom.constructor;

public interface NodeConstructorRegistry {

    <T> void setConstructor(Class<T> classType, NodeConstructor<? extends T> constructor);

    <T> NodeConstructor<T> getConstructor(Class<T> type);

}
