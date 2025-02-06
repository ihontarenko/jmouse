package org.jmouse.common.support.context;

import org.jmouse.core.reflection.Reflections;

import static java.util.Objects.requireNonNull;

public interface ArgumentsContext {

    default  <T> T requireArgument(Object name) {
        T value = getArgument(name);

        if (value == null) {
            throw new ArgumentNotFoundException("Required argument not found [%s]".formatted(name));
        }

        return value;
    }


    <T> T getArgument(Object name);

    void setArgument(Object name, Object argument);

    default void copyArgument(Object name, ArgumentsContext arguments) {
        this.setArgument(name, arguments.getArgument(name));
    }

    default void setArgument(Object argument) {
        setArgument(Reflections.getUserClass(requireNonNull(argument).getClass()), argument);
    }

    default void setArguments(Object... arguments) {
        for (Object argument : arguments) {
            setArgument(argument);
        }
    }

    boolean hasArgument(Object name);

}
