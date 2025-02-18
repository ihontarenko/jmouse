package org.jmouse.core.bind.introspection.structured;

import org.jmouse.core.bind.introspection.MethodDescriptor;
import org.jmouse.util.Getter;
import org.jmouse.util.Setter;

public interface PropertyDescriptor<T> {

    String getName();

    ObjectDescriptor<T> getOwner();

    Getter<T, Object> getGetter();

    void setGetter(Getter<T, Object> getter);

    Setter<T, Object> getSetter();

    void setSetter(Setter<T, Object> setter);

    default boolean isReadable() {
        return getGetter() != null;
    }

    default boolean isWritable() {
        return getSetter() != null;
    }

    default MethodDescriptor getGetterMethod() {
        throw new UnsupportedOperationException(
                "Unable get getter method for '%s' descriptor".formatted(getName()));
    }

    default void setGetterMethod(MethodDescriptor getterMethod) {
        throw new UnsupportedOperationException(
                "Unable set getter method for '%s' descriptor".formatted(getName()));
    }

    default MethodDescriptor getSetterMethod() {
        throw new UnsupportedOperationException(
                "Unable get setter method for '%s' descriptor".formatted(getName()));
    }

    default void setSetterMethod(MethodDescriptor setterMethod) {
        throw new UnsupportedOperationException(
                "Unable set setter method for '%s' descriptor".formatted(getName()));
    }

}
