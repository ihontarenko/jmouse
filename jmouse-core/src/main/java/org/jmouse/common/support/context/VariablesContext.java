package org.jmouse.common.support.context;

import org.jmouse.core.reflection.Reflections;

import static java.util.Objects.requireNonNull;

public interface VariablesContext {

    default <T> T requireVaruable(Object name) {
        T value = getVariable(name);

        if (value == null) {
            throw new VariableNotFoundException("Required variable not found [%s]".formatted(name));
        }

        return value;
    }

    <T> T getVariable(Object name);

    void setVariable(Object name, Object value);

    default void setVariable(Object variable) {
        setVariable(Reflections.getUserClass(requireNonNull(variable).getClass()), variable);
    }

    default void setVariables(Object... variables) {
        for (Object variable : variables) {
            setVariable(variable);
        }
    }

}
